package compiler;

import org.mdkt.compiler.DynamicClassLoader;
import org.mdkt.compiler.ExtendedStandardJavaFileManager;

import javax.tools.JavaFileManager;

public class IMJFileManager extends ExtendedStandardJavaFileManager {
    protected IMJFileManager(JavaFileManager fileManager, DynamicClassLoader cl) {
        super(fileManager, cl);
    }
}
