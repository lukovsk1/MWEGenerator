package fragment;

public abstract class ACodeFragment<T> implements ICodeFragment {

	private final String m_path;

	private final T m_content;
	private final long m_fragmentNumber;

	public ACodeFragment(String path, T content, long fragmentNumber) {
		m_path = path;
		m_content = content;
		m_fragmentNumber = fragmentNumber;
	}

	public String getPath() {
		return m_path;
	}

	public T getContent() {
		return m_content;
	}

	@Override
	public long getFragmentNumber() {
		return m_fragmentNumber;
	}
}
