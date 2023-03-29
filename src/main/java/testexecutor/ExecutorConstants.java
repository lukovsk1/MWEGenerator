package testexecutor;

public final class ExecutorConstants {

    public static final TestExecutorOptions CALCULATOR_OPTIONS = new TestExecutorOptions()
            .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
            .withSourceFolderPath("src")
            .withUnitTestFolderPath("test")
            .withUnitTestMethod("calculator.CalculatorTest#testCalculator")
            .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
            .withNumberOfThreads(1);

    public static final TestExecutorOptions CALCULATOR_OPTIONS_MULTI = new TestExecutorOptions()
            .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
            .withSourceFolderPath("src")
            .withUnitTestFolderPath("test")
            .withUnitTestMethod("calculator.CalculatorTest#testCalculator")
            .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
            .withMultipleRuns(true)
            .withNumberOfThreads(1);

    public static final TestExecutorOptions FIBONACCI_OPTIONS = new TestExecutorOptions()
            .withModulePath(System.getProperty("user.dir") + "\\FibonacciExample")
            .withSourceFolderPath("src")
            .withUnitTestFolderPath("test")
            .withUnitTestMethod("FibonacciNumberTest#testFibonacci")
            .withExpectedResult("java.lang.StackOverflowError");

    public static final TestExecutorOptions FIBONACCI_OPTIONS_MULTI = new TestExecutorOptions()
            .withModulePath(System.getProperty("user.dir") + "\\FibonacciExample")
            .withSourceFolderPath("src")
            .withUnitTestFolderPath("test")
            .withUnitTestMethod("FibonacciNumberTest#testFibonacci")
            .withExpectedResult("java.lang.StackOverflowError")
            .withMultipleRuns(true);


    public static final TestExecutorOptions SIMPLE_EXAMPLE_OPTIONS = new TestExecutorOptions()
            .withModulePath(System.getProperty("user.dir") + "\\SimpleExample")
            .withSourceFolderPath("src")
            .withUnitTestFolderPath("test")
            .withUnitTestMethod("SimpleExampleTest#testExecution")
            .withExpectedResult("org.opentest4j.AssertionFailedError");

    public static final TestExecutorOptions SIMPLE_EXAMPLE_OPTIONS_MULTI = new TestExecutorOptions()
            .withModulePath(System.getProperty("user.dir") + "\\SimpleExample")
            .withSourceFolderPath("src")
            .withUnitTestFolderPath("test")
            .withUnitTestMethod("SimpleExampleTest#testExecution")
            .withExpectedResult("org.opentest4j.AssertionFailedError")
            .withMultipleRuns(true);
}
