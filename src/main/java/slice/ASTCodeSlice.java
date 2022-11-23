package slice;

import utility.JavaParserUtility.Token;

import java.util.ArrayList;
import java.util.List;

public class ASTCodeSlice extends ACodeSlice<Void> implements IHierarchicalCodeSlice {

	private final List<Token> m_tokens = new ArrayList<>();
	private final List<ASTCodeSlice> m_children = new ArrayList<>();
	private int m_level = -1;

	public ASTCodeSlice(String path, int sliceNumber) {
		super(path, null, sliceNumber);
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

	public void addChild(ASTCodeSlice child) {
		m_children.add(child);
	}

	@Override
	public List<ASTCodeSlice> getChildren() {
		return new ArrayList<>(m_children);
	}

	public void setLevel(int level) {
		this.m_level = level;
	}

	@Override
	public int getLevel() {
		return m_level;
	}
}
