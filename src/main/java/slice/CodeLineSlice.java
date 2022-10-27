package slice;

public class CodeLineSlice implements ICodeSlice {

    private final String m_path;

    private final String m_codeLine;
    private final int m_lineNumber;
    private final int m_sliceNumber;

    public CodeLineSlice(String path, String codeLine, int lineNumber, int sliceNumber) {
        m_path = path;
        m_codeLine = codeLine;
        m_lineNumber = lineNumber;
        m_sliceNumber = sliceNumber;
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

    @Override
    public int getSliceNumber() {
        return m_sliceNumber;
    }
}
