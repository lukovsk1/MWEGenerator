package generator;

import slice.ICodeSlice;
import slice.IHierarchicalCodeSlice;
import testexecutor.ATestExecutorOptions;
import testexecutor.ITestExecutor;
import testexecutor.ast.ASTTestExecutor;
import testexecutor.ast.ASTTestExecutorOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ASTMWEGenerator extends AbstractMWEGenerator {

	private int m_level = 0;

	public void runGenerator(boolean multipleRuns) {
		// extract code slices
		ASTTestExecutor executor = getTestExecutor();
		var fullTree = executor.extractSlices();
		List<ICodeSlice> slicing = new ArrayList<>(fullTree);
		System.out.println("############## RUNNING TEST ##############");
		m_level = 0;


		while (true) {
			System.out.println("############## EXECUTING LVL " + m_level + " ##############");
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

		System.out.println("Recreating result in testingoutput folder...");
		executor.recreateCode(slicing);
		System.out.println("############## FINISHED ##############");
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
		System.out.println(sb);
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
		ASTTestExecutorOptions options = (ASTTestExecutorOptions) new ASTTestExecutorOptions()
				.withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
				.withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
				.withUnitTestMethod("calculator.CalculatorTest#testCalculator")
				.withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
				.withCompilationType(ATestExecutorOptions.ECompilationType.IN_MEMORY);
		return new ASTTestExecutor(options);
	}
}
