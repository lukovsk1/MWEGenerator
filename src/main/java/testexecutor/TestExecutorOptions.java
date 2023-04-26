package testexecutor;

public class TestExecutorOptions {

	public enum ELogLevel {
		NONE(0),
		INFO(1),
		DEBUG(2);

		private final int m_index;

		ELogLevel(int index) {
			m_index = index;
		}

		public boolean shouldLog(ELogLevel lvl) {
			return lvl.m_index <= m_index;
		}
	}

	private String m_modulePath;
	private String m_sourceFolderPath;
	private String m_unitTestFolderPath;
	private String m_unitTestMethod;
	private String m_expectedResult;
	private ECompilationType m_compilationType = ECompilationType.IN_MEMORY;
	private ELogLevel m_logLevel = ELogLevel.INFO;
	private boolean m_logCompilationErrors = false;
	private boolean m_logRuntimeErrors = false;
	private boolean m_multipleRuns = false;
	private int m_numberOfThreads = 1;
	private boolean m_preSliceCode = true;
	private int m_graphAlgorithmFragmentLimit = 0;
	private boolean m_graphAlgorithmEscalatingFragmentLimit = false;

	public TestExecutorOptions withModulePath(String modulePath) {
		m_modulePath = modulePath;
		return this;
	}

	public String getModulePath() {
		return m_modulePath;
	}

	public String getSourceFolderPath() {
		return m_sourceFolderPath;
	}

	public TestExecutorOptions withSourceFolderPath(String sourceFolderPath) {
		m_sourceFolderPath = sourceFolderPath;
		return this;
	}

	public String getUnitTestFolderPath() {
		return m_unitTestFolderPath;
	}

	public TestExecutorOptions withUnitTestFolderPath(String unitTestFolderPath) {
		m_unitTestFolderPath = unitTestFolderPath;
		return this;
	}

	public TestExecutorOptions withExpectedResult(String expectedResult) {
		m_expectedResult = expectedResult;
		return this;
	}

	public String getExpectedResult() {
		return m_expectedResult;
	}

	public TestExecutorOptions withUnitTestMethod(String unitTestMethod) {
		m_unitTestMethod = unitTestMethod;
		return this;
	}

	public String getUnitTestMethod() {
		return m_unitTestMethod;
	}

	public TestExecutorOptions withCompilationType(ECompilationType compilationType) {
		m_compilationType = compilationType;
		return this;
	}

	public ECompilationType getCompilationType() {
		return m_compilationType;
	}

	public TestExecutorOptions withLogging(ELogLevel logLevel) {
		m_logLevel = logLevel;
		return this;
	}

	public ELogLevel getLogLevel() {
		return m_logLevel;
	}

	public TestExecutorOptions withLogCompilationErrors(boolean logCompilationErrors) {
		m_logCompilationErrors = logCompilationErrors;
		return this;
	}

	public boolean isLogCompilationErrors() {
		return m_logCompilationErrors;
	}

	public TestExecutorOptions withLogRuntimeErrors(boolean logRuntimeErrors) {
		m_logRuntimeErrors = logRuntimeErrors;
		return this;
	}

	public boolean isLogRuntimeErrors() {
		return m_logRuntimeErrors;
	}

	public TestExecutorOptions withMultipleRuns(boolean multipleRuns) {
		m_multipleRuns = multipleRuns;
		return this;
	}

	public boolean isMultipleRuns() {
		return m_multipleRuns;
	}

	public TestExecutorOptions withNumberOfThreads(int numberOfThreads) {
		m_numberOfThreads = numberOfThreads;
		return this;
	}

	public int getNumberOfThreads() {
		return m_numberOfThreads;
	}

	public TestExecutorOptions withPreSliceCode(boolean preSliceCode) {
		m_preSliceCode = preSliceCode;
		return this;
	}

	public boolean isPreSliceCode() {
		return m_preSliceCode;
	}

	public TestExecutorOptions withGraphAlgorithmFragmentLimit(int limit) {
		m_graphAlgorithmFragmentLimit = limit;
		return this;
	}

	public int getGraphAlgorithmFragmentLimit() {
		return m_graphAlgorithmFragmentLimit;
	}

	public TestExecutorOptions withEscalatingFragmentLimit(boolean flag) {
		m_graphAlgorithmEscalatingFragmentLimit = flag;
		return this;
	}

	public boolean isEscalatingFragmentLimit() {
		return m_graphAlgorithmEscalatingFragmentLimit;
	}

	@Override
	public String toString() {
		return "{\n" +
				"\tm_modulePath='" + m_modulePath + "',\n" +
				"\tm_sourceFolderPath='" + m_sourceFolderPath + "',\n" +
				"\tm_unitTestFolderPath='" + m_unitTestFolderPath + "',\n" +
				"\tm_unitTestMethod='" + m_unitTestMethod + "',\n" +
				"\tm_expectedResult='" + m_expectedResult + "',\n" +
				"\tm_compilationType=" + m_compilationType + ",\n" +
				"\tm_logLevel=" + m_logLevel + ",\n" +
				"\tm_logCompilationErrors=" + m_logCompilationErrors + ",\n" +
				"\tm_logRuntimeErrors=" + m_logRuntimeErrors + ",\n" +
				"\tm_multipleRuns=" + m_multipleRuns + ",\n" +
				"\tm_numberOfThreads=" + m_numberOfThreads + ",\n" +
				"\tm_preSliceCode=" + m_preSliceCode + ",\n" +
				"\tm_graphAlgorithmFragmentLimit=" + m_graphAlgorithmFragmentLimit + ",\n" +
				"\tm_graphAlgorithmEscalatingFragmentLimit=" + m_graphAlgorithmEscalatingFragmentLimit + ",\n" +
				'}';
	}

    public enum ECompilationType {
		COMMAND_LINE,
		IN_MEMORY
	}
}
