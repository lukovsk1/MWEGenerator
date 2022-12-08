package generator;

import testexecutor.TestExecutorOptions;
import testexecutor.ITestExecutor;
import testexecutor.singlecharacter.SingleCharacterTestExecutor;

public class SingleCharacterMWEGenerator extends AbstractMWEGenerator {
    public SingleCharacterMWEGenerator(TestExecutorOptions options) {
        super(options);
    }

    @Override
    protected ITestExecutor getTestExecutor() {
        return new SingleCharacterTestExecutor(m_testExecutorOptions);
    }
}
