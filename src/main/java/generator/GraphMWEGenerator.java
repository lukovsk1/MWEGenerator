package generator;

import testexecutor.TestExecutorOptions;
import testexecutor.graph.GraphTestExecutor;

public class GraphMWEGenerator extends ASTMWEGenerator {
    public GraphMWEGenerator(TestExecutorOptions options) {
        super(options);
    }

    @Override
    protected GraphTestExecutor getTestExecutor() {
        return new GraphTestExecutor(m_testExecutorOptions);
    }
}
