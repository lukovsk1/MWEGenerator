package testexecutor;

public interface ITestExecutorBuilder {

	<T extends ATestExecutorOptions> T build();
}
