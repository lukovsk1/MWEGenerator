package slice;

public abstract class ACodeSlice<T> implements ICodeSlice {

	private final String m_path;

	private final T m_content;
	private final int m_sliceNumber;

	public ACodeSlice(String path, T content, int sliceNumber) {
		m_path = path;
		m_content = content;
		m_sliceNumber = sliceNumber;
	}

	public String getPath() {
		return m_path;
	}

	public T getContent() {
		return m_content;
	}

	@Override
	public int getSliceNumber() {
		return m_sliceNumber;
	}
}
