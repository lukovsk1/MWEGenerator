package compiler;

import org.mdkt.compiler.DynamicClassLoader;

import javax.tools.JavaFileManager;

/**
 * Adapted from {@link org.mdkt.compiler.ExtendedStandardJavaFileManager}.
 * Expose constructor.
 */
public class ExtendedStandardJavaFileManager extends org.mdkt.compiler.ExtendedStandardJavaFileManager {

    protected ExtendedStandardJavaFileManager(JavaFileManager fileManager, DynamicClassLoader cl) {
        super(fileManager, cl);
    }
}
