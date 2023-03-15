import generator.ASTMWEGenerator;
import generator.AbstractMWEGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

	public static void main(String[] args) throws IOException {
		// write both to the console and a log file
		String dir = System.getProperty("user.dir");
		DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		File logFile = new File(dir + "/logs/" + LocalDateTime.now().format(timeStampPattern) + ".log");
		if (logFile.createNewFile()) {
			FileOutputStream fos = new FileOutputStream(logFile);
			TeeOutputStream newOut = new TeeOutputStream(System.out, fos);
			System.setOut(new PrintStream(newOut, true));
		} else {
			System.out.println("ERROR: Unable to create log file " + logFile);
		}

		AbstractMWEGenerator generator;
		if (args.length >= 5) {
			TestExecutorOptions options = new TestExecutorOptions()
					.withModulePath(args[0])
					.withSourceFolderPath(args[1])
					.withUnitTestFolderPath(args[2])
					.withUnitTestMethod(args[3])
					.withExpectedResult(args[4])
					.withLogCompilationErrors(false)
					.withLogRuntimeErrors(false)
					.withNumberOfThreads(16)
					.withPreSliceCode(false)
					.withLogging(TestExecutorOptions.ELogLevel.INFO);

			generator = new ASTMWEGenerator(options);
		} else {
			generator = new ASTMWEGenerator(Constants.CALCULATOR_OPTIONS_MULTI);
		}

		try {
			long start = System.currentTimeMillis();

			generator.runGenerator();

			long time = System.currentTimeMillis() - start;
			System.out.println();
			System.out.println("TOTAL EXECUTION TIME: " + time + " ms.");

			// check output size:
			long outputSize = FileUtils.sizeOfDirectory(new File(dir + "/testingoutput"));
			System.out.println("TOTAL OUTPUT SIZE: " + outputSize + " bytes");
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}
}
