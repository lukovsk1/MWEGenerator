package generator;

import fragment.ICodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.graph.GraphTestExecutor;
import utility.CollectionsUtility;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GraphMWEGenerator extends AbstractMWEGenerator {

    private int m_level = 0;

    public GraphMWEGenerator(TestExecutorOptions options) {
        super(options);
    }

    @Override
    protected GraphTestExecutor getTestExecutor() {
        return new GraphTestExecutor(m_testExecutorOptions);
    }

    public void runGenerator() {
        try {
            // extract code fragments
            GraphTestExecutor executor = getTestExecutor();
            executor.initialize();
            executor.extractFragments();

            int numberOfFragments = executor.getNumberOfFragmentsInDB();
            logInfo("Extracted " + numberOfFragments + " fragments to graph database");

            logInfo("############## RUNNING TEST ##############");
            while (true) {
                long start = System.currentTimeMillis();
                logInfo("############## EXECUTING LVL " + m_level + " ##############");
                List<ICodeFragment> fragments = executor.getActiveFragments();
                if (fragments.isEmpty()) {
                    break;
                }
                List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
                logInfo("Level " + m_level + " took " + (System.currentTimeMillis() - start) + "ms");
                printConfigurationInfo(minConfig, fragments);
                executor.addFixedFragments(minConfig);
                executor.addDiscardedFragments(CollectionsUtility.listMinus(fragments, minConfig));
                m_level++;
            }

            logInfo("Recreating result in testingoutput folder...");
            executor.recreateCode(Collections.emptyList());
            logInfo("############## FINISHED ##############");
        } finally {
            cleanup();
        }
    }


    @Override
    protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeFragment> configuration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
        ITestExecutor.ETestResult result = executor.test(configuration);
        log(":::: " + result + " :::: level: " + m_level + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: ", result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
        return result;
    }

    @Override
    protected void checkPreconditions(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
        // only check precondition initially
        if (m_level == 0) {
            super.checkPreconditions(executor, initialConfiguration, totalFragments, resultMap);
        }
    }
}
