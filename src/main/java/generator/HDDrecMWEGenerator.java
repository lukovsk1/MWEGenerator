package generator;

import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.hdd.HDDrecTestExecutor;
import utility.CollectionsUtility;
import utility.StatsUtility;

import java.util.*;
import java.util.concurrent.CancellationException;

public class HDDrecMWEGenerator extends HDDMWEGenerator {

	public HDDrecMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator() {
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
					long levelStart = System.currentTimeMillis();
					IHierarchicalCodeFragment currentFragment = executor.getQueue().poll();
					if (currentFragment == null) {
						continue;
					}
					List<ICodeFragment> fragments = CollectionsUtility.castList(currentFragment.getChildren(), ICodeFragment.class);
					if (fragments.isEmpty()) {
						continue;
					}
					logInfo("############## EXECUTING LVL " + m_testNr + "-" + m_level + " ##############");
					List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
					logInfo("Level " + m_testNr + "-" + m_level + " took " + StatsUtility.formatDuration(levelStart));
					printConfigurationInfo(minConfig, fragments);
					executor.addFixedFragments(minConfig);
					executor.getQueue().addAll(CollectionsUtility.castList(minConfig, IHierarchicalCodeFragment.class));
					long numberOfFragments = executor.getQueue()
							.stream()
							.filter(Objects::nonNull)
							.mapToLong(fr -> {
								Set<IHierarchicalCodeFragment> subTree = CollectionsUtility.getChildrenInDeep(fr);
								return subTree.size() - 1;
							})
							.sum();
					logInfo("############## After level " + m_level + " there are " + (executor.getFixedFragments().size() + numberOfFragments) + " / " + m_initialNumberOfFragments + " fragments left :::: " + executor.getStatistics());
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
		log(":::: " + result + " :::: level: " + m_testNr + "-" + m_level + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: " + executor.getStatistics(), result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
		return result;
	}

	@Override
	protected HDDrecTestExecutor getTestExecutor() {
		return new HDDrecTestExecutor(m_testExecutorOptions);
	}
}
