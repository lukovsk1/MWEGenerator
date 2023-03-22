package fragment;

import utility.JavaParserUtility.Token;

import java.util.ArrayList;
import java.util.List;

public class GraphCodeFragment extends ACodeFragment<Void> {

    private final List<Token> m_tokens;

    public GraphCodeFragment(String path, long fragmentNumber, List<Token> tokens) {
        super(path, null, fragmentNumber);
        m_tokens = new ArrayList<>(tokens);
    }

    public List<Token> getTokens() {
        return new ArrayList<>(m_tokens);
    }
}
