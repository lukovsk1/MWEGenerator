package testexecutor;

import slice.ICodeSlice;

import java.util.List;

public class NullTestExecutor implements ITestExecutor {

    @Override
    public ETestState test(List<ICodeSlice> slices) {
        return ETestState.ERROR;
    }
}
