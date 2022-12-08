import generator.ASTMWEGenerator;
import generator.CodeLineMWEGenerator;
import generator.SingleCharacterMWEGenerator;
import testexecutor.TestExecutorOptions;

public class Main {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        TestExecutorOptions calculatorOptions = new TestExecutorOptions()
                .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
                .withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
                .withUnitTestMethod("calculator.CalculatorTest#testCalculator")
                .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
                .withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY);

        TestExecutorOptions fibonacciOptions = new TestExecutorOptions()
                .withModulePath(System.getProperty("user.dir") + "\\FibonacciExample")
                .withUnitTestFilePath("test\\FibonacciNumberTest.java")
                .withUnitTestMethod("FibonacciNumberTest#testFibonacci")
                .withExpectedResult("java.lang.StackOverflowError")
                .withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY);

        // new SingleCharacterMWEGenerator().runGenerator(false);
        // new CodeLineMWEGenerator().runGenerator(true);
        new ASTMWEGenerator(calculatorOptions).runGenerator(false);

        long time = System.currentTimeMillis() - start;
        System.out.println();
        System.out.println("TOTAL EXECUTION TIME: " + time + " ms.");
    }
}
