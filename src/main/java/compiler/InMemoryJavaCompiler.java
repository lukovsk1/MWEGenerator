//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package compiler;

import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.ExtendedStandardJavaFileManager;
import org.mdkt.compiler.SourceCode;

import java.io.Writer;
import java.nio.charset.Charset;
import java.util.*;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Adapted from {@link org.mdkt.compiler.InMemoryJavaCompiler}.
 * Using own implementation of class loader.
 */
public class InMemoryJavaCompiler {
    private JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    private DynamicClassLoader classLoader = new DynamicClassLoader(ClassLoader.getSystemClassLoader());
    private Iterable<String> options;
    boolean ignoreWarnings = false;
    private Map<String, SourceCode> sourceCodes = new HashMap();

    public static InMemoryJavaCompiler newInstance() {
        return new InMemoryJavaCompiler();
    }

    private InMemoryJavaCompiler() {
    }

    public InMemoryJavaCompiler useParentClassLoader(ClassLoader parent) {
        this.classLoader = new DynamicClassLoader(parent);
        return this;
    }

    public ClassLoader getClassloader() {
        return this.classLoader;
    }

    public InMemoryJavaCompiler useOptions(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    public InMemoryJavaCompiler ignoreWarnings() {
        this.ignoreWarnings = true;
        return this;
    }

    public Map<String, Class<?>> compileAll() throws Exception {
        if (this.sourceCodes.size() == 0) {
            throw new CompilationException("No source code to compile");
        } else {
            Collection<SourceCode> compilationUnits = this.sourceCodes.values();
            CompiledCode[] code = new CompiledCode[compilationUnits.size()];
            Iterator<SourceCode> iter = compilationUnits.iterator();

            for(int i = 0; i < code.length; ++i) {
                code[i] = new CompiledCode(((SourceCode)iter.next()).getClassName());
            }

            DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector();
            ExtendedStandardJavaFileManager fileManager = new compiler.ExtendedStandardJavaFileManager(this.javac.getStandardFileManager((DiagnosticListener)null, (Locale)null, (Charset)null), this.classLoader);
            JavaCompiler.CompilationTask task = this.javac.getTask((Writer)null, fileManager, collector, this.options, (Iterable)null, compilationUnits);
            boolean result = task.call();
            if (!result || collector.getDiagnostics().size() > 0) {
                StringBuffer exceptionMsg = new StringBuffer();
                exceptionMsg.append("Unable to compile the source");
                boolean hasWarnings = false;
                boolean hasErrors = false;
                Iterator var11 = collector.getDiagnostics().iterator();

                while(true) {
                    if (!var11.hasNext()) {
                        if (hasWarnings && !this.ignoreWarnings || hasErrors) {
                            throw new CompilationException(exceptionMsg.toString());
                        }
                        break;
                    }

                    Diagnostic<? extends JavaFileObject> d = (Diagnostic)var11.next();
                    switch (d.getKind()) {
                        case NOTE:
                        case MANDATORY_WARNING:
                        case WARNING:
                            hasWarnings = true;
                            break;
                        case OTHER:
                        case ERROR:
                        default:
                            hasErrors = true;
                    }

                    exceptionMsg.append("\n").append("[kind=").append(d.getKind());
                    exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
                    exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append("]");
                }
            }

            Map<String, Class<?>> classes = new HashMap();
            Iterator var15 = this.sourceCodes.keySet().iterator();

            while(var15.hasNext()) {
                String className = (String)var15.next();
                classes.put(className, this.classLoader.loadClass(className));
            }

            return classes;
        }
    }

    public Class<?> compile(String className, String sourceCode) throws Exception {
        return (Class)this.addSource(className, sourceCode).compileAll().get(className);
    }

    public InMemoryJavaCompiler addSource(String className, String sourceCode) throws Exception {
        // if a source is re-added in exactly the same way as before, remove it from compilation.
        // It is already in the class loader.
        if(this.sourceCodes.get(className) != null
                && this.sourceCodes.get(className).getCharContent(false).equals(sourceCode)) {
            this.sourceCodes.remove(className);
        }

        this.sourceCodes.put(className, new SourceCode(className, sourceCode));
        return this;
    }

    /**
     * Remove all class names not present in the given collection from source codes and class loader
     *
     * @param classNames the class names to keep
     */
    public void pruneSources(Collection<String> classNames) {
        Iterator<Map.Entry<String, SourceCode>> it = this.sourceCodes.entrySet().iterator();
        while(it.hasNext()) {
            String cn = it.next().getKey();
            if (!classNames.contains(cn)) {
                it.remove();
                this.classLoader.removeClass(cn);
            }
        }
    }
}
