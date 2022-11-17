package slice;

import utility.JavaParserUtility.Token;

import java.util.ArrayList;
import java.util.List;

public class ASTCodeSlice extends ACodeSlice<Void> {

	private final List<Token> m_tokens = new ArrayList<>();

	public ASTCodeSlice(String path, int sliceNumber) {
		super(path, null, sliceNumber);
	}

	public void addToken(Token token) {
		m_tokens.add(token);
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
}
