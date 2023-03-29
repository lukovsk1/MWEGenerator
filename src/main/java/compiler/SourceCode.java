package compiler;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

/**
 * Adapted from {@link org.mdkt.compiler.SourceCode}.
 * Saving hash of content
 */
public class SourceCode extends SimpleJavaFileObject {
    private String contents = null;
    private String className;
    private int contentHash;
    private boolean isFixed;

    public SourceCode(String className, String contents, boolean fixed) throws Exception {
        super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.contents = contents;
        this.className = className;
        this.contentHash = contents.hashCode();
        this.isFixed = fixed;
    }

    public String getClassName() {
        return this.className;
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return this.contents;
    }

    public int getContentHash() {
        return contentHash;
    }

    public boolean isFixed() {
        return isFixed;
    }
}
