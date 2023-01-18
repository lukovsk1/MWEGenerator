import testexecutor.TestExecutorOptions;

public final class Constants {

	public static final TestExecutorOptions CALCULATOR_OPTIONS = new TestExecutorOptions()
			.withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
			.withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
			.withUnitTestMethod("calculator.CalculatorTest#testCalculator")
			.withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
			.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
			.withLogging(true);

	public static final TestExecutorOptions CALCULATOR_OPTIONS_MULTI = new TestExecutorOptions()
			.withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
			.withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
			.withUnitTestMethod("calculator.CalculatorTest#testCalculator")
			.withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
			.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
			.withMultipleRuns(true)
			.withLogging(true);

	public static final TestExecutorOptions FIBONACCI_OPTIONS = new TestExecutorOptions()
			.withModulePath(System.getProperty("user.dir") + "\\FibonacciExample")
			.withUnitTestFilePath("test\\FibonacciNumberTest.java")
			.withUnitTestMethod("FibonacciNumberTest#testFibonacci")
			.withExpectedResult("java.lang.StackOverflowError")
			.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
			.withLogging(true);

	public static final TestExecutorOptions FIBONACCI_OPTIONS_MULTI = new TestExecutorOptions()
			.withModulePath(System.getProperty("user.dir") + "\\FibonacciExample")
			.withUnitTestFilePath("test\\FibonacciNumberTest.java")
			.withUnitTestMethod("FibonacciNumberTest#testFibonacci")
			.withExpectedResult("java.lang.StackOverflowError")
			.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
			.withMultipleRuns(true)
			.withLogging(true);


	public static final TestExecutorOptions SIMPLE_EXAMPLE_OPTIONS = new TestExecutorOptions()
			.withModulePath(System.getProperty("user.dir") + "\\SimpleExample")
			.withUnitTestFilePath("test\\SimpleExampleTest.java")
			.withUnitTestMethod("SimpleExampleTest#testExecution")
			.withExpectedResult("org.opentest4j.AssertionFailedError")
			.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
			.withLogging(true);

	public static final TestExecutorOptions SIMPLE_EXAMPLE_OPTIONS_MULTI = new TestExecutorOptions()
			.withModulePath(System.getProperty("user.dir") + "\\SimpleExample")
			.withUnitTestFilePath("test\\SimpleExampleTest.java")
			.withUnitTestMethod("SimpleExampleTest#testExecution")
			.withExpectedResult("org.opentest4j.AssertionFailedError")
			.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
			.withLogging(true);
}
