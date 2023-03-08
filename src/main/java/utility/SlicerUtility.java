package utility;

import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.IntSet;
import compiler.InMemoryJavaCompiler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SlicerUtility {
    private SlicerUtility() {
    }

    public static void doSlicing(Path testSourcePath, TestExecutorOptions executorOptions) {
        try {
            // compile source and unit test folder
            File sourceFolder = new File(testSourcePath + "\\" + executorOptions.getSourceFolderPath());
            File unitTestFolder = new File(testSourcePath + "\\" + executorOptions.getUnitTestFolderPath());
            File targetDir = new File(testSourcePath + "\\compiledClasses");
            InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance().ignoreWarnings();

            FileUtility.addJavaFilesToCompiler(compiler, sourceFolder.toPath());
            FileUtility.addJavaFilesToCompiler(compiler, unitTestFolder.toPath());
            compiler.compileAll();

            if (!targetDir.mkdir()) {
                throw new SlicingException("Unable to recreate file " + targetDir);
            }
            compiler.getFileManager().copyCompiledCodeToFolder(targetDir);

            // create analysis scope
            AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
            scope.addToScope(ClassLoaderReference.Application, new SourceDirectoryTreeModule(sourceFolder));
            scope.addToScope(ClassLoaderReference.Application, new SourceDirectoryTreeModule(unitTestFolder));
            scope.addToScope(ClassLoaderReference.Application, new BinaryDirectoryTreeModule(targetDir));

            // add standard libraries to scope
            String[] stdlibs = WalaProperties.getJ2SEJarFiles();
            for (String stdlib : stdlibs) {
                scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
            }

            // add required class path dependencies to scope
            String classpath = System.getProperty("java.class.path");
            for (String jarPath : StringUtils.split(classpath, ";")) {
                if (jarPath.endsWith("\\junit-platform-console-standalone-1.9.1.jar")) {
                    scope.addToScope(ClassLoaderReference.Primordial, new JarFile(jarPath));
                }
            }

            // extracting class hierarchy
            ClassHierarchy cha = ClassHierarchyFactory.make(scope);
            System.out.println("Slicing: built class hierarchy with " + cha.getNumberOfClasses() + " classes");
            System.out.println("Slicing: \n" + Warnings.asString());
            Warnings.clear();

            // define entry point for slicing
            MethodReference entryRef = extractUnitTestMethodReference(executorOptions.getUnitTestMethod());
            DefaultEntrypoint entryPoint = new DefaultEntrypoint(entryRef, cha);

            AnalysisOptions options = new AnalysisOptions();
            options.setEntrypoints(Collections.singleton(entryPoint));
            options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);

            IAnalysisCacheView cache = new AnalysisCacheImpl();
            CallGraphBuilder<InstanceKey> builder = new ZeroCFABuilderFactory().make(options, cache, cha);

            // creating call graph
            System.out.println("Slicing: building call and system dependence graphs...");
            long start = System.currentTimeMillis();
            CallGraph cg = builder.makeCallGraph(options, null);
            PointerAnalysis<InstanceKey> ptr = builder.getPointerAnalysis();

            // TODO: find correct slicing options
            Slicer.DataDependenceOptions data = Slicer.DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS;
            Slicer.ControlDependenceOptions control = Slicer.ControlDependenceOptions.NONE;
            //Slicer.DataDependenceOptions data = Slicer.DataDependenceOptions.FULL;
            //Slicer.ControlDependenceOptions control = Slicer.ControlDependenceOptions.FULL;

            SDG<InstanceKey> sdg = new SDG<>(cg, ptr, new AstJavaModRef<>(), data, control);
            System.out.println("Slicing: ... in " + (System.currentTimeMillis() - start) + "ms");
            System.out.println("Slicing: computing code slice...");
            start = System.currentTimeMillis();

            // running slicing algorithm
            Collection<Statement> slicingStatements = getSlicingStatements(entryRef, cg);
            Collection<Statement> codeSlice = Slicer.computeBackwardSlice(sdg, slicingStatements);
            System.out.println("Slicing: Extracted slicing with " + codeSlice.size() + " statements in " + (System.currentTimeMillis() - start) + "ms");

            // removing unused code from source folder
            FileUtility.deleteFolder(targetDir.toPath());
            pruneSourceFolder(codeSlice, testSourcePath);
        } catch (Exception e) {
            throw new SlicingException("Error while pre-slicing code", e);
        }
    }

    private static Collection<Statement> getSlicingStatements(MethodReference entryRef, CallGraph cg) {
        Set<CGNode> unitTestNodes = cg.getNodes(entryRef);
        List<Statement> statements = new ArrayList<>();
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
        return statements;
    }

    private static MethodReference extractUnitTestMethodReference(String unitTestMethod) {
        String[] unitTestName = unitTestMethod.split("#");
        String className = "L" + unitTestName[0].replaceAll("\\.", "/");
        return MethodReference.findOrCreate(ClassLoaderReference.Application, className, unitTestName[1], "()V");
    }


    public static void pruneSourceFolder(Collection<Statement> slice, Path testSourcePath) throws IOException {
        Map<IClass, Set<IMethod>> methodsByClass = slice.stream()
                .map(Statement::getNode)
                .map(CGNode::getMethod)
                .collect(Collectors.groupingBy(IMember::getDeclaringClass, Collectors.mapping(fr -> fr, Collectors.toSet())));

        System.out.println("Slicing: created slicing");
        for (Map.Entry<IClass, Set<IMethod>> entry : methodsByClass.entrySet()) {
            System.out.println("Slicing: Class " + entry.getKey().getName().toString());
            for (IMethod method : entry.getValue()) {
                System.out.println("Slicing: \tMethod " + method.getSelector().toString());
            }
        }

        // TODO: early exit
        if (1 == 1) {
            return;
        }

        List<String> classes = methodsByClass.keySet().stream()
                .filter(cl -> cl.getClassLoader().getReference().equals(ClassLoaderReference.Application))
                .map(cl -> cl.getReference().getName().toString().substring(1))
                .map(name -> name.replace("/", "\\"))
                .map(name -> name + ".java")
                .collect(Collectors.toList());

        // TODO extract more dependencies from slice
        try (Stream<Path> walk = Files.walk(testSourcePath)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> Files.isRegularFile(path) && "java".equals(FilenameUtils.getExtension(path.toString())))
                    .filter(path -> classes.stream().noneMatch(fn -> path.toString().endsWith(fn)))
                    .map(Path::toFile)
                    .forEach(File::delete);
        }

        // TODO remove unused methods

        // retry compilation
        InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance().ignoreWarnings();
        FileUtility.addJavaFilesToCompiler(compiler, testSourcePath);
        try {
            compiler.compileAll();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
