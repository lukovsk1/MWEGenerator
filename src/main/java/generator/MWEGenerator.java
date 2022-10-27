package generator;

import testexecutor.CodeLineTestExecutor;
import testexecutor.CodeLineTestExecutorOptions;
import testexecutor.ITestExecutor;
import slice.ICodeSlice;
import utility.ListUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MWEGenerator {

    public static void main(String[] args) {

        // extract code slices
        ITestExecutor executor = getTestExecutor();
        List<ICodeSlice> slices = new ArrayList<>(executor.extractSlices());

        int totalSlices = slices.size();

        assert executeTest(executor, Collections.emptyList(), totalSlices) != ITestExecutor.ETestResult.FAILED;
        assert executeTest(executor, slices, totalSlices) == ITestExecutor.ETestResult.FAILED;

        // ddmin algorithm
        int granularity = 2;
        while(slices.size() >= 2) {
            List<List<ICodeSlice>> subsets = ListUtility.split(slices, granularity);
            assert subsets.size() == granularity;

            boolean someComplementIsFailing = false;
            for(List<ICodeSlice> subset : subsets) {
                List<ICodeSlice> complement = ListUtility.listMinus(slices, subset);

                if(executeTest(executor, complement, totalSlices) == ITestExecutor.ETestResult.FAILED) {
                    slices = complement;
                    granularity = Math.max(granularity - 1, 2);
                    someComplementIsFailing = true;
                }
            }

            if(!someComplementIsFailing) {
                if(granularity == slices.size()) {
                    break;
                }

                granularity = Math.min(granularity * 2, slices.size());
            }
        }

        // TODO present result
        // log to console / write to file(s)
    }

    private static ITestExecutor.ETestResult executeTest(ITestExecutor executor, List<ICodeSlice> slices, int totalSlices) {
        ITestExecutor.ETestResult result =  executor.test(slices);

        String slicingIdentifier = getSlicingIdentifier(slices, totalSlices);

        System.out.print(slicingIdentifier);
        System.out.print(" -> ");
        System.out.println(result);

        return result;
    }

    private static String getSlicingIdentifier(List<ICodeSlice> slices, int totalSlices) {
        List<Integer> activeSlices = IntStream.range(0,totalSlices)
                .mapToObj(i -> 0)
                .collect(Collectors.toList());

        slices.forEach(sl -> activeSlices.set(sl.getSliceNumber(), 1));

        return activeSlices.stream().map(i -> Integer.toString(i)).collect(Collectors.joining());
    }

    private static ITestExecutor getTestExecutor() {
        CodeLineTestExecutorOptions options = new CodeLineTestExecutorOptions()
                .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
                .withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
                .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>");
        return new CodeLineTestExecutor(options);
    }
}
