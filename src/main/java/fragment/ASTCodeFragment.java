package fragment;

import utility.JavaParserUtility.Token;

import java.util.ArrayList;
import java.util.List;

public class ASTCodeFragment extends ACodeFragment<Void> implements IHierarchicalCodeFragment {

	private final List<Token> m_tokens = new ArrayList<>();
	private final List<ASTCodeFragment> m_children = new ArrayList<>();
	private int m_level = -1;

	public ASTCodeFragment(String path, int fragmentNumber) {
		super(path, null, fragmentNumber);
	}

	public void addToken(Token token) {
		m_tokens.add(token);
	}

	public List<Token> getTokens() {
		return new ArrayList<>(m_tokens);
	}

	public int getStart() {
		if (m_tokens.isEmpty()) {
			return -1;
		}
		return m_tokens.get(0).start;
	}

	public int getEnd() {
		if (m_tokens.isEmpty()) {
			return -1;
		}
		return m_tokens.get(m_tokens.size() - 1).end;
	}

	public void addChild(ASTCodeFragment child) {
		if (this != child) {
			m_children.add(child);
		}
	}

	@Override
	public List<ASTCodeFragment> getChildren() {
		return new ArrayList<>(m_children);
	}

	public void setLevel(int level) {
		this.m_level = level;
	}

	@Override
	public int getLevel() {
		return m_level;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("# ");
		sb.append(getFragmentNumber());
		sb.append(" # ");
		if (!m_tokens.isEmpty()) {
			sb.append(m_tokens.get(0).node);
		}
		return sb.toString();
	}
}
