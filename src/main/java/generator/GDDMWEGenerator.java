package generator;

import fragment.ICodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.gdd.GDDTestExecutor;
import utility.CollectionsUtility;
import utility.StatsUtility;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

public class GDDMWEGenerator extends AbstractMWEGenerator {

    private int m_level = 0;

    public GDDMWEGenerator(TestExecutorOptions options) {
        super(options);
    }

    @Override
    protected GDDTestExecutor getTestExecutor() {
        return new GDDTestExecutor(m_testExecutorOptions);
    }

    public void runGenerator() {
        GDDTestExecutor executor = getTestExecutor();
        try {
            // extract code fragments
            executor.initialize();
            long extractionStart = System.currentTimeMillis();
            executor.extractFragments();
            final int numberOfFragments = executor.getNumberOfFragmentsInDB();
            StatsUtility.getStatsTracker().writeInputFragments(numberOfFragments);
            logInfo("Extracted " + numberOfFragments + " fragments to graph database in " + StatsUtility.formatDuration(extractionStart));
            int numberOfFixedFragments = 0;
            int testNr = 0;
            while (true) {
                long runStart = System.currentTimeMillis();
                logInfo("############## RUNNING TEST NR. " + testNr + " ##############");
                while (true) {
                    long levelStart = System.currentTimeMillis();
                    m_fragments = executor.getActiveFragments();
                    if (m_fragments.isEmpty()) {
                        break;
                    }
                    logInfo("############## EXECUTING LVL " + m_level + " with " + m_fragments.size() + " active fragments ##############");
                    List<ICodeFragment> minConfig = runDDMin(executor, m_fragments, m_fragments.size());
                    logInfo("Level " + m_level + " took " + StatsUtility.formatDuration(levelStart));
                    printConfigurationInfo(minConfig, m_fragments);
                    executor.addFixedFragments(minConfig);
                    executor.addDiscardedFragments(CollectionsUtility.listMinus(m_fragments, minConfig));
                    logInfo("############## After level " + m_level + " there are " + executor.getNumberOfFragmentsInDB() + " / " + numberOfFragments + " fragments left :::: " + executor.getStatistics());
                    m_level++;
                }

                logInfo("Recreating result in testingoutput folder...");
                executor.recreateCode(Collections.emptyList());
                int numberOfFragmentsLeft = executor.getNumberOfFragmentsInDB();
                StatsUtility.getStatsTracker().writeOutputFragments(numberOfFragmentsLeft);
                logInfo("############## FINISHED NR. " + testNr++ + " in " + StatsUtility.formatDuration(runStart) + " :::: Reduced to " + numberOfFragmentsLeft + " out of " + numberOfFragments + " :::: " + executor.getStatistics() + " ##############");
                if (!m_testExecutorOptions.isMultipleRuns() || numberOfFixedFragments == numberOfFragmentsLeft) {
                    break;
                }
                numberOfFixedFragments = numberOfFragmentsLeft;
                executor.changeSourceToOutputFolder();
                if (m_testExecutorOptions.isEscalatingFragmentLimit()) {
                    m_testExecutorOptions.withGraphAlgorithmFragmentLimit(2 * m_testExecutorOptions.getGraphAlgorithmFragmentLimit());
                }
            }
            logInfo("Format result in testingoutput folder...");
            executor.formatOutputFolder();
        } catch (CancellationException e) {
            logInfo("Execution was manually cancelled. Recreate intermediate result in testingoutput folder...");
            if (m_fragments != null && !m_fragments.isEmpty()) {
                executor.recreateCode(m_fragments);
                executor.formatOutputFolder();
                StatsUtility.getStatsTracker().writeOutputFragments(executor.getNumberOfFragmentsInDB());
            }
            throw e;
        } finally {
            cleanup();
        }
    }


    @Override
    protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeFragment> configuration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
        ITestExecutor.ETestResult result = executor.test(configuration);
        log(":::: " + result + " :::: level: " + m_level + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: " + executor.getStatistics(), result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
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
