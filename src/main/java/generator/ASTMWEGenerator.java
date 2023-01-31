package generator;

import fragment.ICodeFragment;
import fragment.IHierarchicalCodeFragment;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.ast.ASTTestExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ASTMWEGenerator extends AbstractMWEGenerator {

	private int m_level = 0;

	public ASTMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator() {
		// extract code fragments
		ASTTestExecutor executor = getTestExecutor();
		executor.initialize();
		List<ICodeFragment> fullTree = executor.extractFragments();
		List<ICodeFragment> fragments = new ArrayList<>(fullTree);
		logInfo("############## RUNNING TEST ##############");
		m_level = 0;


		while (true) {
			logInfo("############## EXECUTING LVL " + m_level + " ##############");
			List<ICodeFragment> minConfig = runDDMin(executor, fragments, fragments.size());
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
		logDebug(":::: " + result + " :::: " + configuration.size() + " / " + totalFragments + " :::: " + configuration.stream().map(fr -> String.valueOf(fr.getFragmentNumber())).collect(Collectors.joining(", ")));
		return result;
	}

	@Override
	protected void checkPreconditions(ITestExecutor executor, List<ICodeFragment> initialConfiguration, int totalFragments, Map<String, ITestExecutor.ETestResult> resultMap) {
		// only check precondition of initial level
		if (m_level == 0) {
			super.checkPreconditions(executor, initialConfiguration, totalFragments, resultMap);
		}
	}

	@Override
	protected ASTTestExecutor getTestExecutor() {
		return new ASTTestExecutor(m_testExecutorOptions);
	}
}
