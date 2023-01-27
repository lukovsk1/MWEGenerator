package testexecutor;

public class TestExecutorOptions {

	public enum ECompilationType {
		COMMAND_LINE,
		IN_MEMORY
	}

	private String m_modulePath;
	private String m_sourceFolderPath;
	private String m_unitTestFolderPath;
	private String m_unitTestMethod;
	private String m_expectedResult;
	private ECompilationType m_compilationType = ECompilationType.IN_MEMORY;
	private boolean m_logging = true;
	private boolean m_logCompilationErrors = false;
	private boolean m_multipleRuns = false;

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

	public TestExecutorOptions withLogging(boolean logging) {
		m_logging = logging;
		return this;
	}

	public boolean isLogging() {
		return m_logging;
	}

	public TestExecutorOptions withLogCompilationErrors(boolean logCompilationErrors) {
		m_logCompilationErrors = logCompilationErrors;
		return this;
	}

	public boolean isLogCompilationErrors() {
		return m_logCompilationErrors;
	}

	public TestExecutorOptions withMultipleRuns(boolean multipleRuns) {
		m_multipleRuns = multipleRuns;
		return this;
	}

	public boolean isMultipleRuns() {
		return m_multipleRuns;
	}
}
