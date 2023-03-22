package testexecutor;

import fragment.ICodeFragment;

import java.util.List;

public interface ITestExecutor {

	enum ETestResult {
		OK,
		FAILED,
		ERROR_COMPILATION,
		ERROR_RUNTIME
	}

	void initialize();

	List<ICodeFragment> extractFragments();

	void recreateCode(List<ICodeFragment> fragments);

	ETestResult test(List<ICodeFragment> fragments);

	// sets the source folder of the executor to the build folder
	// in order to rerun the test with the previous result
	void changeSourceToOutputFolder();

	String getStatistics();
}
