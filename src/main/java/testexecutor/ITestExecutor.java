package testexecutor;

import slice.ICodeSlice;

import java.util.List;

public interface ITestExecutor {

    enum ETestResult {
        OK,
        FAILED,
        ERROR_COMPILATION,
        ERROR_RUNTIME
    }

    List<ICodeSlice> extractSlices();

    void recreateCode(List<ICodeSlice> slices);

    ETestResult test(List<ICodeSlice> slices);
}
