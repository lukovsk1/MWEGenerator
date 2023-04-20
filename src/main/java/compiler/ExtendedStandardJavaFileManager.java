package compiler;

import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.DynamicClassLoader;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapted from {@link org.mdkt.compiler.ExtendedStandardJavaFileManager}.
 * Expose constructor.
 */
public class ExtendedStandardJavaFileManager extends org.mdkt.compiler.ExtendedStandardJavaFileManager {

    private List<CompiledCode> compiledCode = new ArrayList<>();
    private DynamicClassLoader cl;

    protected ExtendedStandardJavaFileManager(JavaFileManager fileManager, DynamicClassLoader cl) {
        super(fileManager, cl);
        this.cl = cl;
    }

    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        try {
            CompiledCode innerClass = new CompiledCode(className);
            this.compiledCode.add(innerClass);
            this.cl.addCode(innerClass);
            System.out.println("Compilation: added " + innerClass.getName());
            return innerClass;
        } catch (Exception var6) {
            throw new RuntimeException("Error while creating in-memory output file for " + className, var6);
        }
    }

    public void copyCompiledCodeToFolder(File folder) throws IOException {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("Invalid folder for copying compiled code");
        }
        for (CompiledCode code : compiledCode) {
            String name = code.getClassName() + ".class";
            File f = new File(folder, name);
            if (f.createNewFile()) {
                Files.write(f.toPath(), code.getByteCode());
            } else {
                throw new RuntimeException("Unable to create new file for compiled code");
            }
        }
    }
}
