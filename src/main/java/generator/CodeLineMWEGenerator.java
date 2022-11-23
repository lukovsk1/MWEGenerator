package generator;

import testexecutor.ATestExecutorOptions;
import testexecutor.ITestExecutor;
import testexecutor.codeline.CodeLineTestExecutor;
import testexecutor.codeline.CodeLineTestExecutorOptions;

public class CodeLineMWEGenerator extends AbstractMWEGenerator {
    @Override
    protected ITestExecutor getTestExecutor() {
        CodeLineTestExecutorOptions options = (CodeLineTestExecutorOptions) new CodeLineTestExecutorOptions()
                .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
                .withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
                .withUnitTestMethod("calculator.CalculatorTest#testCalculator")
                .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
                .withCompilationType(ATestExecutorOptions.ECompilationType.IN_MEMORY);
        return new CodeLineTestExecutor(options);
    }
}
