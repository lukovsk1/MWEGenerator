package compiler;

import org.mdkt.compiler.*;

import javax.tools.*;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.*;

public class IMJCompiler {
    private JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    private DynamicClassLoader classLoader = new DynamicClassLoader(ClassLoader.getSystemClassLoader());
    private Iterable<String> options;
    boolean ignoreWarnings = false;
    private Map<String, SourceCode> sourceCodes = new HashMap();

    public static IMJCompiler newInstance() {
        return new IMJCompiler();
    }

    private IMJCompiler() {
    }

    public IMJCompiler useParentClassLoader(ClassLoader parent) {
        this.classLoader = new DynamicClassLoader(parent);
        return this;
    }

    public ClassLoader getClassloader() {
        return this.classLoader;
    }

    public IMJCompiler useOptions(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    public IMJCompiler ignoreWarnings() {
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
            ExtendedStandardJavaFileManager fileManager = new IMJFileManager(this.javac.getStandardFileManager((DiagnosticListener)null, (Locale)null, (Charset)null), this.classLoader);
            JavaCompiler.CompilationTask task = this.javac.getTask((Writer)null, fileManager, collector, this.options, (Iterable)null, compilationUnits);
            boolean result = task.call();
            // ignore warnings if compilation task was sucessfull
            if (!result && collector.getDiagnostics().size() > 0) {
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

    public IMJCompiler addSource(String className, String sourceCode) throws Exception {
        this.sourceCodes.put(className, new SourceCode(className, sourceCode));
        return this;
    }
}
