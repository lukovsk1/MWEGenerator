package utility;

import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.*;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.IntSet;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;

public final class SlicerUtility {

    private SlicerUtility() {
    }


    public static void doSlicing(Path testSourcePath, String unitTestMethod) {
        try {
            AnalysisScope scope = AnalysisScopeReader.instance.makeJavaBinaryAnalysisScope(testSourcePath.toString(), null);

            // add the whole classpath to the scope
            String classpath = System.getProperty("java.class.path");
            for (String jarPath : StringUtils.split(classpath, ";")) {
                if (jarPath.endsWith("\\junit-platform-console-standalone-1.9.1.jar")) {
                    scope.addToScope(ClassLoaderReference.Primordial, new JarFile(jarPath));
                }
            }

            ClassHierarchy cha = ClassHierarchyFactory.make(scope);
            System.out.println("Slicing: " + cha.getNumberOfClasses() + " classes");
            System.out.println("Slicing: \n" + Warnings.asString());
            Warnings.clear();
            MethodReference entryRef = extractUnitTestMethodReference(unitTestMethod);
            DefaultEntrypoint entryPoint = new DefaultEntrypoint(entryRef, cha);
            AnalysisOptions options = new AnalysisOptions();
            options.setEntrypoints(Collections.singleton(entryPoint));
            options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);
            IAnalysisCacheView cache = new AnalysisCacheImpl();
            CallGraphBuilder<InstanceKey> builder = new ZeroCFABuilderFactory().make(options, cache, cha);
            System.out.println("Slicing: building call graph...");
            CallGraph cg = builder.makeCallGraph(options, null);
            PointerAnalysis<InstanceKey> ptr = builder.getPointerAnalysis();
            Slicer.DataDependenceOptions data = Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
            Slicer.ControlDependenceOptions control = Slicer.ControlDependenceOptions.NONE;
            SDG<InstanceKey> sdg = new SDG<>(cg, ptr, new AstJavaModRef<>(), data, control);
            Statement slicingStatement = getSlicingStatement(entryRef, cg);
            Collection<Statement> codeSlice = Slicer.computeBackwardSlice(sdg, slicingStatement);
            System.out.println("Slicing: Extracted slicing with " + codeSlice.size() + " statements");
            dumpSlice(codeSlice);
        } catch (Exception e) {
            throw new SlicingException("Error while pre-slicing code", e);
        }
    }

    private static Statement getSlicingStatement(MethodReference entryRef, CallGraph cg) {
        Set<CGNode> unitTestNodes = cg.getNodes(entryRef);
        List<NormalStatement> statements = new ArrayList<>();
        for (CGNode node : unitTestNodes) {
            IR ir = node.getIR();
            for (SSAInstruction s : ir.getInstructions()) {
                if (s instanceof SSAAbstractInvokeInstruction) {
                    SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
                    IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
                    statements.add(new NormalStatement(node, indices.intIterator().next()));
                }
            }
        }
        statements.sort(Comparator.comparing(StatementWithInstructionIndex::getInstructionIndex));

        // returns the last statement of the unit test method.
        // TODO: return the expected failing statement
        return statements.get(statements.size() - 1);
    }

    private static MethodReference extractUnitTestMethodReference(String unitTestMethod) {
        String[] unitTestName = unitTestMethod.split("#");
        String className = "L" + unitTestName[0].replaceAll("\\.", "/");
        return MethodReference.findOrCreate(ClassLoaderReference.Application, className, unitTestName[1], "()V");
    }


    public static void dumpSlice(Collection<Statement> slice) {
        for (Statement s : slice) {
            System.err.println(s);
        }
    }
}
