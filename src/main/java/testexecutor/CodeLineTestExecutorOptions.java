package testexecutor;

public class CodeLineTestExecutorOptions {

    private String m_modulePath;
    private String m_unitTestFilePath;
    private String m_unitTestMethod;
    private String m_expectedResult;

    public CodeLineTestExecutorOptions() {

    }

    public CodeLineTestExecutorOptions withModulePath(String modulePath) {
        m_modulePath = modulePath;
        return this;
    }

    public String getModulePath() {
        return m_modulePath;
    }

    public CodeLineTestExecutorOptions withUnitTestFilePath(String unitTestFilePath) {
        m_unitTestFilePath = unitTestFilePath;
        return this;
    }

    public String getUnitTestFilePath() {
        return m_unitTestFilePath;
    }

    public CodeLineTestExecutorOptions withExpectedResult(String expectedResult) {
        m_expectedResult = expectedResult;
        return this;
    }
    public String getExpectedResult() {
        return m_expectedResult;
    }

    public CodeLineTestExecutorOptions withUnitTestMethod(String unitTestMethod) {
        m_unitTestMethod = unitTestMethod;
        return this;
    }

    public String getUnitTestMethod() {
        return m_unitTestMethod;
    }
}
