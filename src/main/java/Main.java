import generator.AbstractMWEGenerator;
import generator.GraphMWEGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;
import testexecutor.ExecutorConstants;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		// write both to the console and a log file
		String dir = System.getProperty("user.dir");
		DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		String formattedDate = LocalDateTime.now().format(timeStampPattern);
		File logFile = new File(dir + "/logs/_" + formattedDate + ".log");
		FileOutputStream fos = null;
		if (logFile.createNewFile()) {
			fos = new FileOutputStream(logFile);
			TeeOutputStream newOut = new TeeOutputStream(System.out, fos);
			System.setOut(new PrintStream(newOut, true));
		} else {
			System.out.println("ERROR: Unable to create log file " + logFile);
		}

		AbstractMWEGenerator generator = null;
		if (args.length == 0) {
			generator = new GraphMWEGenerator(ExecutorConstants.CALCULATOR_OPTIONS_MULTI);
		} else if (args.length == 1) {
			Class<?> generatorClass = Class.forName(args[0]);
			Constructor<?> constructor = generatorClass.getConstructor(TestExecutorOptions.class);
			generator = (AbstractMWEGenerator) constructor.newInstance(ExecutorConstants.CALCULATOR_OPTIONS_MULTI);
		} else if (args.length >= 6) {
			Class<?> generatorClass = Class.forName(args[0]);
			Constructor<?> constructor = generatorClass.getConstructor(TestExecutorOptions.class);
			TestExecutorOptions options = new TestExecutorOptions()
					.withModulePath(args[1])
					.withSourceFolderPath(args[2])
					.withUnitTestFolderPath(args[3])
					.withUnitTestMethod(args[4])
					.withExpectedResult(args[5])
					.withLogCompilationErrors(false)
					.withLogRuntimeErrors(false)
					.withNumberOfThreads(16)
					.withPreSliceCode(false)
					.withMultipleRuns(true)
					.withLogging(TestExecutorOptions.ELogLevel.INFO);

			if (args.length >= 7) {
				options.withGraphAlgorithmFragmentLimit(Integer.parseInt(args[6]));
			}

			generator = (AbstractMWEGenerator) constructor.newInstance(options);
		} else {
			System.out.println("ERROR: Invalid number of arguments");
			System.exit(1);
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

		if (fos != null) {
			fos.close();
			File finalLogFile = new File(dir + "/logs/" + formattedDate + ".log");
			Files.move(logFile.toPath(), finalLogFile.toPath());
		}
	}
}
