package generator;

import fragment.HDDCodeFragment;
import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.hdd.HDDTestExecutor;
import utility.CollectionsUtility;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class HDDMWEGenerator extends AbstractMWEGenerator {

	protected int m_testNr = 0;
	protected int m_level = 0;
	protected int m_maxLevel = 0;
	protected long m_initialNumberOfFragments = 0;

	public HDDMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator() {
		HDDTestExecutor executor = getTestExecutor();
		try {
			// extract code fragments
			executor.initialize();
			List<ICodeFragment> fullTree = executor.extractFragments();
			List<ICodeFragment> fragments = new ArrayList<>(fullTree);
			analyzeTree(fullTree);

			int numberOfFixedFragments = 0;
			while (true) {
				m_level = 0;
				long runStart = System.currentTimeMillis();
				logInfo("############## RUNNING TEST NR. " + m_testNr + " ##############");
				while (true) {
					long start = System.currentTimeMillis();
					logInfo("############## EXECUTING LVL " + m_testNr + "-" + m_level + " / " + m_maxLevel + " ##############");
					List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
					logInfo("Level " + m_testNr + "-" + m_level + " / " + m_maxLevel + " took " + (System.currentTimeMillis() - start) + "ms");
					printConfigurationInfo(minConfig, fragments);
					if (minConfig.isEmpty()) {
						break;
					}
					executor.addFixedFragments(minConfig);
					AtomicLong numberOfFragments = new AtomicLong();
					fragments = minConfig.stream()
							.map(fr -> (IHierarchicalCodeFragment) fr)
							.map(IHierarchicalCodeFragment::getChildren)
							.flatMap(Collection::stream)
							.peek(fr -> numberOfFragments.addAndGet(CollectionsUtility.getChildrenInDeep(fr).size()))
							.map(fr -> (ICodeFragment) fr)
							.collect(Collectors.toList());
					logInfo("############## After level " + m_level + " there are " + (executor.getFixedFragments().size() + numberOfFragments.get()) + " / " + m_initialNumberOfFragments + " fragments left :::: " + executor.getStatistics());
					m_level++;
				}

				logInfo("Recreating result in testingoutput folder...");
				executor.recreateCode(fragments);
				int numberOfFragmentsLeft = executor.getFixedFragments().size();
				logInfo("############## FINISHED NR. " + m_testNr++ + " in " + (System.currentTimeMillis() - runStart) + "ms :::: Reduced to " + numberOfFragmentsLeft + " out of " + m_initialNumberOfFragments + " :::: " + executor.getStatistics() + " ##############");
				if (!m_testExecutorOptions.isMultipleRuns() || numberOfFixedFragments == numberOfFragmentsLeft) {
					break;
				}
				numberOfFixedFragments = numberOfFragmentsLeft;
				executor.changeSourceToOutputFolder();
				Set<ICodeFragment> fixed = executor.getFixedFragments();
				fragments = fullTree.stream().filter(fixed::contains).collect(Collectors.toList());
				fixed.clear();
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
		m_initialNumberOfFragments = numberOfFragments.get();
		System.out.println("Total :::: fragments: " + m_initialNumberOfFragments);
	}

	@Override
	protected HDDTestExecutor getTestExecutor() {
		return new HDDTestExecutor(m_testExecutorOptions);
	}
}
