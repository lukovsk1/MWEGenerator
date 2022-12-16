import generator.ASTMWEGenerator;
import generator.CodeLineMWEGenerator;
import org.apache.commons.io.FileUtils;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.util.concurrent.*;

public class Main {

	public static void main(String[] args) {
		TestExecutorOptions calculatorOptions = new TestExecutorOptions()
				.withModulePath(System.getProperty("user.dir") + "\\CalculatorExample")
				.withUnitTestFilePath("test\\calculator\\CalculatorTest.java")
				.withUnitTestMethod("calculator.CalculatorTest#testCalculator")
				.withExpectedResult("org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <calculator.DividedByZeroException> but was: <java.lang.ArithmeticException>")
				.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
				.withLogging(true);

		TestExecutorOptions fibonacciOptions = new TestExecutorOptions()
				.withModulePath(System.getProperty("user.dir") + "\\FibonacciExample")
				.withUnitTestFilePath("test\\FibonacciNumberTest.java")
				.withUnitTestMethod("FibonacciNumberTest#testFibonacci")
				.withExpectedResult("java.lang.StackOverflowError")
				.withCompilationType(TestExecutorOptions.ECompilationType.IN_MEMORY)
				.withLogging(true);

		try (ExecutorService executor = Executors.newCachedThreadPool()) {
			long start = System.currentTimeMillis();

			executor.submit(() -> {
				//new SingleCharacterMWEGenerator(calculatorOptions).runGenerator(false); // Timed out
				new CodeLineMWEGenerator(calculatorOptions).runGenerator(false); // 6815ms 841 bytes
				//new CodeLineMWEGenerator(calculatorOptions).runGenerator(true); // 8945ms 726 bytes
				//new ASTMWEGenerator(calculatorOptions).runGenerator(false); // 6122ms 718 bytes

				//new SingleCharacterMWEGenerator(fibonacciOptions).runGenerator(false); // Timed out
				//new CodeLineMWEGenerator(fibonacciOptions).runGenerator(false); // 3905ms 943 bytes
				//new CodeLineMWEGenerator(fibonacciOptions).runGenerator(true); // 4194ms 943 bytes
				//new ASTMWEGenerator(fibonacciOptions).runGenerator(false); // 3225ms 863 bytes
			}).get(5, TimeUnit.MINUTES);

			long time = System.currentTimeMillis() - start;
			System.out.println();
			System.out.println("TOTAL EXECUTION TIME: " + time + " ms.");

			// check output size:
			String dir = System.getProperty("user.dir");
			long outputSize = FileUtils.sizeOfDirectory(new File(dir + "/testingoutput"));
			System.out.println("TOTAL OUTPUT SIZE: " + outputSize + " bytes");
		} catch (ExecutionException e) {
			System.out.println("ERROR:" + e);
		} catch (InterruptedException e) {
			System.out.println("INTERRUPTED:" + e);
		} catch (TimeoutException e) {
			System.out.println("TIMED OUT:" + e);
		}
	}
}
