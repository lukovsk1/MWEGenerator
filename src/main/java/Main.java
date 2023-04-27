import generator.AbstractMWEGenerator;
import generator.GDDMWEGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;
import testexecutor.ExecutorConstants;
import testexecutor.TestExecutorOptions;
import utility.StatsTracker;
import utility.StatsUtility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, InterruptedException {
		// write both to the console and a log file
		String dir = System.getProperty("user.dir");
		DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
		String formattedDate = LocalDateTime.now().format(timeStampPattern);
		File logFile = new File(dir + File.separator + "logs" + File.separator + "_" + formattedDate + ".log");
		FileOutputStream fos = null;
		if (logFile.createNewFile()) {
			fos = new FileOutputStream(logFile);
			TeeOutputStream newOut = new TeeOutputStream(System.out, fos);
			System.setOut(new PrintStream(newOut, true));
		} else {
			System.out.println("ERROR: Unable to create log file " + logFile);
		}

		StatsTracker statTracker = StatsUtility.initStatsTracker(formattedDate);

		AbstractMWEGenerator generator;
		if (args.length == 0) {
			generator = new GDDMWEGenerator(ExecutorConstants.CALCULATOR_OPTIONS_MULTI);
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
					.withLogging(TestExecutorOptions.ELogLevel.INFO);

			if (args.length >= 7) {
				options.withMultipleRuns(Boolean.parseBoolean(args[6]));
			}
			if (args.length >= 8) {
				options.withGraphAlgorithmFragmentLimit(Integer.parseInt(args[7]));
			}
			if (args.length >= 9) {
				options.withEscalatingFragmentLimit(Boolean.parseBoolean(args[8]));
			}

			generator = (AbstractMWEGenerator) constructor.newInstance(options);
		} else {
			generator = null;
			System.out.println("ERROR: Invalid number of arguments");
			System.exit(1);
		}

		// read console input
		Scanner sc = new Scanner(System.in);
		Thread readerThread = new Thread(() -> {
			while (!Thread.interrupted()) {
				if (sc.hasNextLine()) {
					String input = sc.nextLine();
					if ("stop".equals(input)) {
						generator.cancelAndWriteIntermediateResult();
					}
				}
			}
		});
		readerThread.start();
		long start = System.currentTimeMillis();

		try {
			generator.runGenerator();
			System.out.println("#### MWE GENERATOR FINISHED SUCCESSFULLY ####");

		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			System.out.println();
			System.out.println("TOTAL EXECUTION TIME: " + StatsUtility.formatDuration(start) + ".");

			// check output size:
			long outputSize = FileUtils.sizeOfDirectory(new File(dir + File.separator + "testingoutput"));
			System.out.println("TOTAL OUTPUT SIZE: " + outputSize + " bytes");
			try {
				statTracker.saveStats(start);
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}

		if (fos != null) {
			fos.close();
			File finalLogFile = new File(dir + File.separator + "logs" + File.separator + formattedDate + ".log");
			Files.move(logFile.toPath(), finalLogFile.toPath());
		}

		// cancel all remaining threads
		System.exit(0);
	}
}
