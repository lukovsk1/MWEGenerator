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
import java.util.stream.Collectors;

public class ASTMWEGenerator extends AbstractMWEGenerator {

	private int m_level = 0;
	private int m_maxLevel = 0;

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
				fragments = minConfig.stream()
						.map(fr -> (IHierarchicalCodeFragment) fr)
						.map(IHierarchicalCodeFragment::getChildren)
						.flatMap(Collection::stream)
						.map(fr -> (ICodeFragment) fr)
						.collect(Collectors.toList());
				m_level++;
			}

			logInfo("Recreating result in testingoutput folder...");
			executor.recreateCode(fragments);
			logInfo("############## FINISHED ##############");
		} finally {
			cleanup();
		}
	}

	private void printConfigurationInfo(List<ICodeFragment> minConfig, List<ICodeFragment> fragments) {
		StringBuilder sb = new StringBuilder();
		for (ICodeFragment fr : fragments) {
			if (minConfig.contains(fr)) {
				sb.append(1);
			} else {
				sb.append(0);
			}
		}
		logInfo(sb);
	}

	@Override
	protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeFragment> configuration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		ITestExecutor.ETestResult result = executor.test(configuration);
		log(":::: " + result + " :::: level: " + m_level + " / " + m_maxLevel + " :::: size: " + configuration.size() + " / " + totalFragments + " :::: " + configuration.stream().map(fr -> String.valueOf(fr.getFragmentNumber())).collect(Collectors.joining(", ")), result == ITestExecutor.ETestResult.FAILED ? TestExecutorOptions.ELogLevel.INFO : TestExecutorOptions.ELogLevel.DEBUG);
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
		m_maxLevel = tree.stream()
				.map(fr -> ((ASTCodeFragment) fr))
				.flatMap(fr -> CollectionsUtility.getChildrenInDeep(fr).stream())
				.collect(Collectors.groupingBy(IHierarchicalCodeFragment::getLevel, Collectors.counting()))
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.peek(e -> System.out.println("Level: " + e.getKey() + " ### fragments: " + e.getValue()))
				.mapToInt(Map.Entry::getKey)
				.max()
				.orElse(0);
	}

	@Override
	protected ASTTestExecutor getTestExecutor() {
		return new ASTTestExecutor(m_testExecutorOptions);
	}
}
