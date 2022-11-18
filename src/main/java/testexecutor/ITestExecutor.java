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

	// sets the source folder of the executor to the build folder
	// in order to rerun the test with the previous result
	void changeSourceToOutputFolder();
}
