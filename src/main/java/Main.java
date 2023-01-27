import generator.ASTMWEGenerator;
import generator.AbstractMWEGenerator;
import org.apache.commons.io.FileUtils;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {

	public static void main(String[] args) {

		AbstractMWEGenerator generator;
		if(args.length >= 4) {
			TestExecutorOptions options = new TestExecutorOptions()
					.withModulePath(args[0])
					.withUnitTestFolderPath(args[1])
					.withUnitTestMethod(args[2])
					.withExpectedResult(args[3])
					//.withCompilationType(TestExecutorOptions.ECompilationType.COMMAND_LINE)
					.withLogCompilationErrors(true);

			generator = new ASTMWEGenerator(options);
		} else {
			generator = new ASTMWEGenerator(Constants.CALCULATOR_OPTIONS);
		}

		generator.runGenerator();
		/*
		try (ExecutorService executor = Executors.newCachedThreadPool()) {
			long start = System.currentTimeMillis();

			executor.submit(generator::runGenerator).get(10, TimeUnit.MINUTES);

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

		 */
	}
}
