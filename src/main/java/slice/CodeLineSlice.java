package slice;

public class CodeLineSlice implements ICodeSlice {

    private final String m_path;

    private final String m_codeLine;
    private final int m_lineNumber;

    public CodeLineSlice(String path, String codeLine, int lineNumber) {
        m_path = path;
        m_codeLine = codeLine;
        m_lineNumber = lineNumber;
    }

    public String getPath() {
        return m_path;
    }

    public String getCodeLine() {
        return m_codeLine;
    }

    public int getLineNumber() {
        return m_lineNumber;
    }
}
