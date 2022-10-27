package generator;

import testexecutor.CodeLineTestExecutor;
import testexecutor.CodeLineTestExecutorOptions;
import testexecutor.ITestExecutor;
import slice.ICodeSlice;
import utility.ListUtility;

import java.util.ArrayList;
import java.util.List;

public class MWEGenerator {

    public static void main(String[] args) {

        // extract code slices
        ITestExecutor executor = getTestExecutor();
        List<ICodeSlice> slices = new ArrayList<>(executor.extractSlices());

        assert executor.test(slices) == ITestExecutor.ETestResult.FAILED;

        // ddmin algorithm
        int granularity = 2;
        while(slices.size() >= 2) {
            List<List<ICodeSlice>> subsets = ListUtility.split(slices, granularity);
            assert subsets.size() == granularity;

            boolean someComplementIsFailing = false;
            for(List<ICodeSlice> subset : subsets) {
                List<ICodeSlice> complement = ListUtility.listMinus(slices, subset);

                if(executor.test(complement) == ITestExecutor.ETestResult.FAILED) {
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

    public static ITestExecutor getTestExecutor() {
        CodeLineTestExecutorOptions options = new CodeLineTestExecutorOptions()
                .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
                .withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
                .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>");
        return new CodeLineTestExecutor(options);
    }
}
