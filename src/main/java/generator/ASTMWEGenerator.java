package generator;

import slice.ICodeSlice;
import slice.IHierarchicalCodeSlice;
import testexecutor.ITestExecutor;
import testexecutor.TestExecutorOptions;
import testexecutor.ast.ASTTestExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ASTMWEGenerator extends AbstractMWEGenerator {

	private int m_level = 0;

	public ASTMWEGenerator(TestExecutorOptions options) {
		super(options);
	}

	public void runGenerator(boolean multipleRuns) {
		// extract code slices
		ASTTestExecutor executor = getTestExecutor();
		var fullTree = executor.extractSlices();
		List<ICodeSlice> slicing = new ArrayList<>(fullTree);
		log("############## RUNNING TEST ##############");
		m_level = 0;


		while (true) {
			log("############## EXECUTING LVL " + m_level + " ##############");
			var minConfig = runDDMin(executor, slicing, slicing.size());
			printSlicingInfo(minConfig, slicing);
			if (minConfig.isEmpty()) {
				break;
			}
			executor.addFixedSlices(minConfig);
			slicing = minConfig.stream()
					.map(sl -> (IHierarchicalCodeSlice) sl)
					.map(IHierarchicalCodeSlice::getChildren)
					.flatMap(Collection::stream)
					.map(sl -> (ICodeSlice) sl)
					.toList();
			m_level++;
		}

		log("Recreating result in testingoutput folder...");
		executor.recreateCode(slicing);
		log("############## FINISHED ##############");
	}

	private void printSlicingInfo(List<ICodeSlice> minConfig, List<ICodeSlice> slicing) {
		StringBuilder sb = new StringBuilder();
		for (var sl : slicing) {
			if (minConfig.contains(sl)) {
				sb.append(1);
			} else {
				sb.append(0);
			}
		}
		log(sb);
	}

	@Override
	protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeSlice> slices, int totalSlices, Map<String, ITestExecutor.ETestResult> resultMap) {
		return executor.test(slices);
	}

	@Override
	protected void checkPreconditions(ITestExecutor executor, List<ICodeSlice> initialSlicing, int totalSlices, Map<String, ITestExecutor.ETestResult> resultMap) {
		// only check precondition of initial level
		if (m_level == 0) {
			super.checkPreconditions(executor, initialSlicing, totalSlices, resultMap);
		}
	}

	@Override
	protected ASTTestExecutor getTestExecutor() {
		return new ASTTestExecutor(m_testExecutorOptions);
	}
}
