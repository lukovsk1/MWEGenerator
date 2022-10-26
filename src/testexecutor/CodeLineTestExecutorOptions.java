package testexecutor;

public class CodeLineTestExecutorOptions {

    private String m_modulePath;

    public CodeLineTestExecutorOptions() {

    }

    public CodeLineTestExecutorOptions withModulePath(String modulePath) {
        m_modulePath = modulePath;
        return this;
    }

    public String getModulePath() {
        return m_modulePath;
    }
}
