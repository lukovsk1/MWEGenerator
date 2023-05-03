package generator;

import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.TestExecutorOptions;
import testexecutor.hdd.HDDTestExecutor;
import testexecutor.hdd.HDDrecTestExecutor;
import utility.CollectionsUtility;
import utility.StatsTracker;
import utility.StatsUtility;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;

public class HDDrecMWEGenerator extends HDDMWEGenerator {

	public HDDrecMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator() {
		StatsTracker statsTracker = StatsUtility.getStatsTracker();
		HDDrecTestExecutor executor = getTestExecutor();
		try {
			int numberOfFixedFragments = 0;
			while (true) {
				// extract code fragments
				executor.initialize();
				List<ICodeFragment> fileRoots = executor.extractFragments();
				analyzeTree(fileRoots);
				m_level = 0;
				long runStart = System.currentTimeMillis();
				logInfo("############## RUNNING TEST NR. " + m_testNr + " ##############");
				while (!executor.getQueue().isEmpty()) {
					m_levelStart = System.currentTimeMillis();
					IHierarchicalCodeFragment currentFragment = executor.getQueue().poll();
					if (currentFragment == null) {
						continue;
					}
					List<ICodeFragment> fragments = CollectionsUtility.castList(currentFragment.getChildren(), ICodeFragment.class);
					if (fragments.isEmpty()) {
						continue;
					}
					logInfo("############## EXECUTING LVL " + m_testNr + "-" + m_level + " ##############");
					statsTracker.startTrackingDDminExecution(m_testNr + "-" + m_level, fragments.size(), calculateTotalNumberOfFragements(executor, fragments));
					List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
					logInfo("Level " + m_testNr + "-" + m_level + " took " + StatsUtility.formatDuration(m_levelStart));
					printConfigurationInfo(minConfig, fragments);
					executor.addFixedFragments(minConfig);
					executor.getQueue().addAll(CollectionsUtility.castList(minConfig, IHierarchicalCodeFragment.class));

					long numberOfRemainingFragments = calculateTotalNumberOfFragements(executor, Collections.emptyList());
					logInfo("############## After level " + m_testNr + "-" + m_level + " there are " + numberOfRemainingFragments + " / " + m_initialNumberOfFragments + " fragments left :::: " + executor.getStatistics());
					executor.trackDDminCompilerStats();
					statsTracker.trackDDminExecutionEnd(m_levelStart, minConfig.size(), numberOfRemainingFragments);
					m_level++;
				}

				logInfo("Recreating result in testingoutput folder...");
				executor.recreateCode(Collections.emptyList());
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

	@Override
	protected long calculateTotalNumberOfFragements(HDDTestExecutor executor0, List<ICodeFragment> activeFragments) {
		HDDrecTestExecutor executor = (HDDrecTestExecutor) executor0;
		long numberOfQueuedFragments = executor.getQueue()
				.stream()
				.filter(Objects::nonNull)
				.mapToLong(fr -> {
					Set<IHierarchicalCodeFragment> subTree = CollectionsUtility.getChildrenInDeep(fr);
					return subTree.size() - 1;
				})
				.sum();
		int numberOfActiveFragments = activeFragments.stream()
				.filter(Objects::nonNull)
				.map(IHierarchicalCodeFragment.class::cast)
				.mapToInt(fr -> CollectionsUtility.getChildrenInDeep(fr).size())
				.sum();
		return executor.getFixedFragments().size() + numberOfQueuedFragments + numberOfActiveFragments;
	}

	@Override
	protected HDDrecTestExecutor getTestExecutor() {
		return new HDDrecTestExecutor(m_testExecutorOptions);
	}
}
