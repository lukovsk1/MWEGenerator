package testexecutor;

public class TestExecutorOptions {

	public enum ECompilationType {
		COMMAND_LINE,
		IN_MEMORY
	}

	private String m_modulePath;
	private String m_unitTestFilePath;
	private String m_unitTestMethod;
	private String m_expectedResult;
	private ECompilationType m_compilationType = ECompilationType.IN_MEMORY;
	private boolean m_logging = true;
	private boolean m_multipleRuns = false;

	public TestExecutorOptions withModulePath(String modulePath) {
		m_modulePath = modulePath;
		return this;
	}

	public String getModulePath() {
		return m_modulePath;
	}

	public TestExecutorOptions withUnitTestFilePath(String unitTestFilePath) {
		m_unitTestFilePath = unitTestFilePath;
		return this;
	}

	public String getUnitTestFilePath() {
		return m_unitTestFilePath;
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

	public TestExecutorOptions withMultipleRuns(boolean multipleRuns) {
		m_multipleRuns = multipleRuns;
		return this;
	}

	public boolean isMultipleRuns() {
		return m_multipleRuns;
	}
}
