package testexecutor;

import slice.ICodeSlice;

import java.util.List;

public interface ITestExecutor {

    enum ETestState {
        OK,
        FAIL,
        ERROR
    }

    ETestState test(List<ICodeSlice> slices);
}
