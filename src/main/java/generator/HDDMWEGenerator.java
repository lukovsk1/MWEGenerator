package generator;

import fragment.HDDCodeFragment;
import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.hdd.HDDTestExecutor;
import utility.CollectionsUtility;
import utility.StatsTracker;
import utility.StatsUtility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class HDDMWEGenerator extends AbstractMWEGenerator {

	protected int m_testNr = 0;
	protected int m_level = 0;
	protected int m_maxLevel = 0;
	protected long m_initialNumberOfFragments = -1;

	public HDDMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator() {
		StatsTracker statsTracker = StatsUtility.getStatsTracker();
		HDDTestExecutor executor = getTestExecutor();
		try {
			// extract code fragments
			int numberOfFixedFragments = 0;
			while (true) {
				executor.initialize();
				List<ICodeFragment> fullTree = executor.extractFragments();
				List<ICodeFragment> fragments = new ArrayList<>(fullTree);
				analyzeTree(fullTree);
				m_level = 0;
				long runStart = System.currentTimeMillis();
				logInfo("############## RUNNING TEST NR. " + m_testNr + " ##############");
				while (true) {
					m_levelStart = System.currentTimeMillis();
					logInfo("############## EXECUTING LVL " + m_testNr + "-" + m_level + " / " + m_maxLevel + " ##############");
					statsTracker.startTrackingDDminExecution(m_testNr + "-" + m_level, fragments.size(), calculateTotalNumberOfFragements(executor, fragments));
					List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
					logInfo("Level " + m_testNr + "-" + m_level + " / " + m_maxLevel + " took " + StatsUtility.formatDuration(m_levelStart));
					printConfigurationInfo(minConfig, fragments);
					executor.addFixedFragments(minConfig);
					fragments = minConfig.stream()
							.map(fr -> (IHierarchicalCodeFragment) fr)
							.map(IHierarchicalCodeFragment::getChildren)
							.flatMap(Collection::stream)
							.map(fr -> (ICodeFragment) fr)
							.collect(Collectors.toList());
					long numberOfRemainingFragments = calculateTotalNumberOfFragements(executor, fragments);
					logInfo("############## After level " + m_testNr + "-" + m_level + " there are " + numberOfRemainingFragments + " / " + m_initialNumberOfFragments + " fragments left :::: " + executor.getStatistics());
					executor.trackDDminCompilerStats();
					statsTracker.trackDDminExecutionEnd(m_levelStart, minConfig.size(), numberOfRemainingFragments);
					if (minConfig.isEmpty()) {
						break;
					}
					m_level++;
				}

				logInfo("Recreating result in testingoutput folder...");
				executor.recreateCode(m_fragments);
				int numberOfFragmentsLeft = executor.getFixedFragments().size();
				logInfo("############## FINISHED NR. " + m_testNr++ + " in " + StatsUtility.formatDuration(runStart) + " :::: Reduced to " + numberOfFragmentsLeft + " out of " + m_initialNumberOfFragments + " :::: " + executor.getStatistics() + " ##############");
				if (!m_testExecutorOptions.isMultipleRuns() || numberOfFixedFragments == numberOfFragmentsLeft) {
					break;
				}
				numberOfFixedFragments = numberOfFragmentsLeft;
				executor.changeSourceToOutputFolder();
				executor.getFixedFragments().clear();
			}
			logInfo("Formatting result in testingoutput folder...");
			executor.formatOutputFolder();
		} catch (CancellationException e) {
			handleRunCancellation(statsTracker, executor);
			throw e;
		} finally {
			cleanup();
		}
	}

	protected void handleRunCancellation(StatsTracker statsTracker, HDDTestExecutor executor) {
		logInfo("Execution was manually cancelled. Track stats and recreate intermediate result in testingoutput folder...");
		executor.trackDDminCompilerStats();
		if (m_fragments != null && !m_fragments.isEmpty()) {
			long numberOfRemainingFragments = calculateTotalNumberOfFragements(executor, m_fragments);
			statsTracker.trackDDminExecutionEnd(m_levelStart, m_fragments.size(), numberOfRemainingFragments);
			executor.recreateCode(m_fragments);
			executor.formatOutputFolder();
		}
	}

	protected long calculateTotalNumberOfFragements(HDDTestExecutor executor, List<ICodeFragment> fragments) {
		return executor.getFixedFragments().size() + fragments.stream()
				.map(IHierarchicalCodeFragment.class::cast)
				.mapToLong(fr -> CollectionsUtility.getChildrenInDeep(fr).size())
				.sum();
	}

	@Override
	protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeFragment> configuration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		ITestExecutor.ETestResult result = executor.test(configuration);
		log(":::: " + result + " :::: level: " + m_testNr + "-" + m_level + " / " + m_maxLevel + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: " + executor.getStatistics(), result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
		return result;
	}

	@Override
	protected void checkPreconditions(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		// only check precondition of initial level
		if (m_level == 0) {
			super.checkPreconditions(executor, initialConfiguration, totalFragments, resultMap);
		}
	}

	protected void analyzeTree(List<ICodeFragment> tree) {
		AtomicLong numberOfFragments = new AtomicLong();
		m_maxLevel = tree.stream()
				.map(fr -> ((HDDCodeFragment) fr))
				.flatMap(fr -> CollectionsUtility.getChildrenInDeep(fr).stream())
				.collect(Collectors.groupingBy(IHierarchicalCodeFragment::getLevel, Collectors.counting()))
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.peek(e -> System.out.println("Level: " + e.getKey() + " :::: fragments: " + e.getValue()))
				.peek(e -> numberOfFragments.addAndGet(e.getValue()))
				.mapToInt(Map.Entry::getKey)
				.max()
				.orElse(0);
		if (m_initialNumberOfFragments == -1) {
			m_initialNumberOfFragments = numberOfFragments.get();
			StatsUtility.getStatsTracker().writeInputFragments(m_initialNumberOfFragments);
		}
		System.out.println("Total :::: fragments: " + numberOfFragments.get() + " out of " + m_initialNumberOfFragments + " originally");
	}

	@Override
	protected HDDTestExecutor getTestExecutor() {
		return new HDDTestExecutor(m_testExecutorOptions);
	}
}
