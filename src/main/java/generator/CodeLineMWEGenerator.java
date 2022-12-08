package generator;

import testexecutor.TestExecutorOptions;
import testexecutor.ITestExecutor;
import testexecutor.codeline.CodeLineTestExecutor;

public class CodeLineMWEGenerator extends AbstractMWEGenerator {
    public CodeLineMWEGenerator(TestExecutorOptions options) {
        super(options);
    }

    @Override
    protected ITestExecutor getTestExecutor() {
        return new CodeLineTestExecutor(m_testExecutorOptions);
    }
}
