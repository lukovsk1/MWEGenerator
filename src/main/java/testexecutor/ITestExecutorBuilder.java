package testexecutor;

public interface ITestExecutorBuilder {

	<T extends TestExecutorOptions> T build();
}
