package generator;

import fragment.ICodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.gdd.GDDTestExecutor;
import utility.CollectionsUtility;
import utility.StatsTracker;
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
        StatsTracker statsTracker = StatsUtility.getStatsTracker();
        GDDTestExecutor executor = getTestExecutor();
        try {
            // extract code fragments
            executor.initialize();
            long extractionStart = System.currentTimeMillis();
            executor.extractFragments();
            final int numberOfFragments = executor.getNumberOfRemainingFragments();
            StatsUtility.getStatsTracker().writeInputFragments(numberOfFragments);
            logInfo("Extracted " + numberOfFragments + " fragments to graph database in " + StatsUtility.formatDuration(extractionStart));
            int numberOfFixedFragments = 0;
            int testNr = 0;
            while (true) {
                long runStart = System.currentTimeMillis();
                logInfo("############## RUNNING TEST NR. " + testNr + " ##############");
                while (true) {
                    long levelStart = System.currentTimeMillis();
                    List<ICodeFragment> fragments = executor.getActiveFragments();
                    if (fragments.isEmpty()) {
                        break;
                    }
                    logInfo("############## EXECUTING LVL " + testNr + "-" + m_level + " with " + fragments.size() + " active fragments ##############");
                    statsTracker.startTrackingDDminExecution(testNr + "-" + m_level, fragments.size(), executor.getNumberOfRemainingFragments());
                    List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
                    logInfo("Level " + testNr + "-" + m_level + " took " + StatsUtility.formatDuration(levelStart));
                    printConfigurationInfo(minConfig, fragments);
                    executor.addFixedFragments(minConfig);
                    executor.addDiscardedFragments(CollectionsUtility.listMinus(fragments, minConfig));
                    long numberOfRemainingFragments = executor.getNumberOfRemainingFragments();
                    logInfo("############## After level " + testNr + "-" + m_level + " there are " + numberOfRemainingFragments + " / " + numberOfFragments + " fragments left :::: " + executor.getStatistics());
                    executor.trackDDminCompilerStats();
                    statsTracker.trackDDminExecutionEnd(levelStart, minConfig.size(), numberOfRemainingFragments);
                    m_level++;
                }

                logInfo("Recreating result in testingoutput folder...");
                executor.recreateCode(Collections.emptyList());
                int numberOfFragmentsLeft = executor.getNumberOfRemainingFragments();
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
