package fragment;

public class CodeLineFragment extends ACodeFragment<String> {

    private final int m_lineNumber;

    public CodeLineFragment(String path, String codeLine, int lineNumber, int fragmentNumber) {
        super(path, codeLine, fragmentNumber);
        m_lineNumber = lineNumber;
    }

    public String getCodeLine() {
        return super.getContent();
    }

    public int getLineNumber() {
        return m_lineNumber;
    }
}
