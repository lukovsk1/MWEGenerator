package generator;

import slice.ICodeSlice;
import testexecutor.ATestExecutorOptions;
import testexecutor.ITestExecutor;
import testexecutor.ast.ASTTestExecutor;
import testexecutor.ast.ASTTestExecutorOptions;
import utility.ListUtility;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MWEGenerator {

	public static void main(String[] args) {
		// extract code slices
		ITestExecutor executor = getTestExecutor();
		List<ICodeSlice> mweSlicing;
		int testNr = 1;
		int totalSlices;
		do {
			System.out.println("############## RUNNING TEST NR. " + testNr++ + " ##############");
			mweSlicing = new ArrayList<>(executor.extractSlices());
			totalSlices = mweSlicing.size();
			long start = System.currentTimeMillis();
			mweSlicing = runDDMin(executor, mweSlicing, totalSlices);
			long time = System.currentTimeMillis() - start;
			System.out.println();
			System.out.println("Found an (locally) minimal slicing (MWE) in " + time + " ms:");
			System.out.println(getSlicingIdentifier(mweSlicing, totalSlices));

			// recreate mwe
			System.out.println("Recreating result in testingoutput folder...");
			executor.recreateCode(mweSlicing);
			executor.changeSourceToOutputFolder();
		} while (mweSlicing.size() < totalSlices);

		System.out.println("############## FINISHED ##############");
	}

	private static List<ICodeSlice> runDDMin(ITestExecutor executor, List<ICodeSlice> mweSlicing, int totalSlices) {
		Map<String, ITestExecutor.ETestResult> resultMap = new HashMap<>();

		if (executeTest(executor, Collections.emptyList(), totalSlices, resultMap) == ITestExecutor.ETestResult.FAILED
				|| executeTest(executor, mweSlicing, totalSlices, resultMap) != ITestExecutor.ETestResult.FAILED) {
			System.out.println("Initial testing conditions are not met.");
			System.exit(1);
		}

		List<ICodeSlice> slices = new ArrayList<>(mweSlicing);
		int granularity = 2;
		while (slices.size() >= 2) {
			List<List<ICodeSlice>> subsets = ListUtility.split(slices, granularity);
			assert subsets.size() == granularity;

			boolean someComplementIsFailing = false;
			for (List<ICodeSlice> subset : subsets) {
				List<ICodeSlice> complement = ListUtility.listMinus(slices, subset);

				if (executeTest(executor, complement, totalSlices, resultMap) == ITestExecutor.ETestResult.FAILED) {
					mweSlicing = new ArrayList<>(complement);
					slices = complement;
					granularity = Math.max(granularity - 1, 2);
					someComplementIsFailing = true;
					break;
				}
			}

			if (!someComplementIsFailing) {
				if (granularity == slices.size()) {
					break;
				}

				granularity = Math.min(granularity * 2, slices.size());
			}
		}
		return mweSlicing;
	}

	private static ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeSlice> slices, int totalSlices, Map<String, ITestExecutor.ETestResult> resultMap) {
		String slicingIdentifier = getSlicingIdentifier(slices, totalSlices);
		ITestExecutor.ETestResult result = resultMap.get(slicingIdentifier);
		if (result != null) {
			return result;
		}

		result = executor.test(slices);

		System.out.print(slicingIdentifier);
		System.out.print(" -> ");
		System.out.println(result);

		resultMap.put(slicingIdentifier, result);

		return result;
	}

	private static String getSlicingIdentifier(List<ICodeSlice> slices, int totalSlices) {
		List<Integer> activeSlices = IntStream.range(0, totalSlices)
				.mapToObj(i -> 0)
				.collect(Collectors.toList());

		slices.forEach(sl -> activeSlices.set(sl.getSliceNumber(), 1));

		return activeSlices.stream().map(i -> Integer.toString(i)).collect(Collectors.joining());
	}

	private static ITestExecutor getTestExecutor() {
		ASTTestExecutorOptions options = (ASTTestExecutorOptions) new ASTTestExecutorOptions()
				.withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
				.withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
				.withUnitTestMethod("calculator.CalculatorTest#testCalculator")
				.withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
				.withCompilationType(ATestExecutorOptions.ECompilationType.IN_MEMORY);
		return new ASTTestExecutor(options);
	}
}
