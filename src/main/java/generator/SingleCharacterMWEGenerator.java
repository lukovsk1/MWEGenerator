package generator;

import testexecutor.ATestExecutorOptions;
import testexecutor.ITestExecutor;
import testexecutor.singlecharacter.SingleCharacterTestExecutor;
import testexecutor.singlecharacter.SingleCharacterTestExecutorOptions;

public class SingleCharacterMWEGenerator extends AbstractMWEGenerator {
    @Override
    protected ITestExecutor getTestExecutor() {
        SingleCharacterTestExecutorOptions options = (SingleCharacterTestExecutorOptions) new SingleCharacterTestExecutorOptions()
                .withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
                .withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
                .withUnitTestMethod("calculator.CalculatorTest#testCalculator")
                .withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
                .withCompilationType(ATestExecutorOptions.ECompilationType.IN_MEMORY);
        return new SingleCharacterTestExecutor(options);
    }
}
