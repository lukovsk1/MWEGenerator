package generator;

import slice.ICodeSlice;
import testexecutor.ITestExecutor;
import utility.CollectionsUtility;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractMWEGenerator {

	public void runGenerator(boolean multipleRuns) {
		// extract code slices
		ITestExecutor executor = getTestExecutor();
		List<ICodeSlice> slicing;
		int testNr = 1;
		int totalSlices;
		do {
			System.out.println("############## RUNNING TEST NR. " + testNr++ + " ##############");
			slicing = executor.extractSlices();
			totalSlices = slicing.size();
			long start = System.currentTimeMillis();
			slicing = runDDMin(executor, slicing, totalSlices);
			long time = System.currentTimeMillis() - start;
			System.out.println();
			System.out.println("Found an (locally) minimal slicing (MWE) in " + time + " ms:");
			System.out.println(getSlicingIdentifier(slicing, totalSlices));

			if(!multipleRuns) {
				break;
			}
			// recreate mwe
			System.out.println("Recreating result in testingoutput folder...");
			executor.recreateCode(slicing);
			executor.changeSourceToOutputFolder();
		} while (slicing.size() < totalSlices);

		System.out.println("############## FINISHED ##############");
	}

	protected List<ICodeSlice> runDDMin(ITestExecutor executor, List<ICodeSlice> initialSlicing, int totalSlices) {
		Map<String, ITestExecutor.ETestResult> resultMap = new HashMap<>();

		checkPreconditions(executor, initialSlicing, totalSlices, resultMap);

		List<ICodeSlice> slices = new ArrayList<>(initialSlicing);
		int granularity = 2;
		while (slices.size() >= 2) {
			List<List<ICodeSlice>> subsets = CollectionsUtility.split(slices, granularity);
			assert subsets.size() == granularity;

			boolean someComplementIsFailing = false;
			for (List<ICodeSlice> subset : subsets) {
				List<ICodeSlice> complement = CollectionsUtility.listMinus(slices, subset);

				if (executeTest(executor, complement, totalSlices, resultMap) == ITestExecutor.ETestResult.FAILED) {
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
		return slices;
	}

	protected void checkPreconditions(ITestExecutor executor, List<ICodeSlice> initialSlicing, int totalSlices, Map<String, ITestExecutor.ETestResult> resultMap) {
		if (executeTest(executor, Collections.emptyList(), totalSlices, resultMap) == ITestExecutor.ETestResult.FAILED
				|| executeTest(executor, initialSlicing, totalSlices, resultMap) != ITestExecutor.ETestResult.FAILED) {
			System.out.println("Initial testing conditions are not met.");
			System.exit(1);
		}
	}

	protected ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeSlice> slices, int totalSlices, Map<String, ITestExecutor.ETestResult> resultMap) {
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

	protected String getSlicingIdentifier(List<ICodeSlice> slices, int totalSlices) {
		List<Integer> activeSlices = IntStream.range(0, totalSlices)
				.mapToObj(i -> 0)
				.collect(Collectors.toList());

		slices.forEach(sl -> activeSlices.set(sl.getSliceNumber(), 1));

		return activeSlices.stream().map(i -> Integer.toString(i)).collect(Collectors.joining());
	}

	protected abstract ITestExecutor getTestExecutor();
}
