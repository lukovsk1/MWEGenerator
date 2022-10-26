package testexecutor;

public class CodeLineTestExecutorOptions {

    private String m_modulePath;
    private String m_unitTestFilePath;

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
}
