package generator;

import fragment.ASTCodeFragment;
import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.ast.ASTTestExecutor;
import utility.CollectionsUtility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class ASTMWEGenerator extends AbstractMWEGenerator {

	private int m_level = 0;
	private int m_maxLevel = 0;
	private long m_initialNumberOfFragments = 0;

	public ASTMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator() {
		try {
			// extract code fragments
			ASTTestExecutor executor = getTestExecutor();
			executor.initialize();
			List<ICodeFragment> fullTree = executor.extractFragments();
			List<ICodeFragment> fragments = new ArrayList<>(fullTree);

			analyzeTree(fullTree);

			logInfo("############## RUNNING TEST ##############");
			long runStart = System.currentTimeMillis();
			m_level = 0;
			while (true) {
				long start = System.currentTimeMillis();
				logInfo("############## EXECUTING LVL " + m_level + " / " + m_maxLevel + " ##############");
				List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
				logInfo("Level " + m_level + " / " + m_maxLevel + " took " + (System.currentTimeMillis() - start) + "ms");
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
			logInfo("Formatting result in testingoutput folder...");
			executor.formatOutputFolder();
			logInfo("############## FINISHED in " + (System.currentTimeMillis() - runStart) + "ms :::: Reduced to " + executor.getFixedFragments().size() + " out of " + m_initialNumberOfFragments + " :::: " + executor.getStatistics() + " ##############");
		} finally {
			cleanup();
		}
	}

	@Override
	protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeFragment> configuration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		ITestExecutor.ETestResult result = executor.test(configuration);
		log(":::: " + result + " :::: level: " + m_level + " / " + m_maxLevel + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: " + executor.getStatistics(), result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
		return result;
	}

	@Override
	protected void checkPreconditions(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		// only check precondition of initial level
		if (m_level == 0) {
			super.checkPreconditions(executor, initialConfiguration, totalFragments, resultMap);
		}
	}

	private void analyzeTree(List<ICodeFragment> tree) {
		AtomicLong numberOfFragments = new AtomicLong();
		m_maxLevel = tree.stream()
				.map(fr -> ((ASTCodeFragment) fr))
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
	protected ASTTestExecutor getTestExecutor() {
		return new ASTTestExecutor(m_testExecutorOptions);
	}
}
