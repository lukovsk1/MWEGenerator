package slice;

public class CodeLineSlice extends ACodeSlice<String> {

    private final int m_lineNumber;

    public CodeLineSlice(String path, String codeLine, int lineNumber, int sliceNumber) {
        super(path, codeLine, sliceNumber);
        m_lineNumber = lineNumber;
    }

    public String getCodeLine() {
        return super.getContent();
    }

    public int getLineNumber() {
        return m_lineNumber;
    }
}
