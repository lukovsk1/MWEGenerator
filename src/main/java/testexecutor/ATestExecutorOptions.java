package testexecutor;

public abstract class ATestExecutorOptions {

    public enum ECompilationType {
        COMMAND_LINE,
        IN_MEMORY
    }

    private String m_modulePath;
    private String m_unitTestFilePath;
    private String m_unitTestMethod;
    private String m_expectedResult;
    private ECompilationType m_compilationType = ECompilationType.IN_MEMORY;

    public ATestExecutorOptions withModulePath(String modulePath) {
        m_modulePath = modulePath;
        return this;
    }

    public String getModulePath() {
        return m_modulePath;
    }

    public ATestExecutorOptions withUnitTestFilePath(String unitTestFilePath) {
        m_unitTestFilePath = unitTestFilePath;
        return this;
    }

    public String getUnitTestFilePath() {
        return m_unitTestFilePath;
    }

    public ATestExecutorOptions withExpectedResult(String expectedResult) {
        m_expectedResult = expectedResult;
        return this;
    }
    public String getExpectedResult() {
        return m_expectedResult;
    }

    public ATestExecutorOptions withUnitTestMethod(String unitTestMethod) {
        m_unitTestMethod = unitTestMethod;
        return this;
    }

    public String getUnitTestMethod() {
        return m_unitTestMethod;
    }

    public ATestExecutorOptions withCompilationType(ECompilationType compilationType) {
        m_compilationType = compilationType;
        return this;
    }

    public ECompilationType getCompilationType() {
        return m_compilationType;
    }
}
