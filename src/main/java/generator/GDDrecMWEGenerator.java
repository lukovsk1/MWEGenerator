package generator;

import testexecutor.TestExecutorOptions;
import testexecutor.gdd.GDDTestExecutor;
import testexecutor.gdd.GDDrecTestExecutor;

public class GDDrecMWEGenerator extends GDDMWEGenerator {

    public GDDrecMWEGenerator(TestExecutorOptions options) {
        super(options);
    }

    @Override
    protected GDDTestExecutor getTestExecutor() {
        return new GDDrecTestExecutor(m_testExecutorOptions);
    }
}
