package generator;

import extractor.IExtractor;
import extractor.IExtractorOptions;
import extractor.NullExtractor;
import slice.ICodeSlice;
import testexecutor.ITestExecutor;
import testexecutor.ITestExecutorOptions;
import testexecutor.NullTestExecutor;
import utility.ListUtility;

import java.util.ArrayList;
import java.util.List;

public class MWEGenerator {

    public static void main(String[] args) {

        // extract code slices
        IExtractor extractor = getExtractor(null);
        List<ICodeSlice> slices = new ArrayList<>(extractor.extractSlices());

        // prepare test executor
        ITestExecutor executor = getTestExecutor(null);

        // ddmin algorithm
        int granularity = 2;
        while(slices.size() >= 2) {
            List<List<ICodeSlice>> subsets = ListUtility.split(slices, granularity);
            assert subsets.size() == granularity;

            boolean someComplementIsFailing = false;
            for(List<ICodeSlice> subset : subsets) {
                List<ICodeSlice> complement = ListUtility.listMinus(slices, subset);

                if(executor.test(complement) == ITestExecutor.ETestState.FAIL) {
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

    public static IExtractor getExtractor(IExtractorOptions options) {
        return new NullExtractor();
    }

    public static ITestExecutor getTestExecutor(ITestExecutorOptions options) {
        return new NullTestExecutor();
    }
}
