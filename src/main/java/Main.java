import generator.ASTMWEGenerator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.concurrent.*;

public class Main {

	public static void main(String[] args) {


		try (ExecutorService executor = Executors.newCachedThreadPool()) {
			long start = System.currentTimeMillis();

			executor.submit(() -> {
				//new SingleCharacterMWEGenerator(Constants.CALCULATOR_OPTIONS).runGenerator(); // Timed out
				//new CodeLineMWEGenerator(Constants.CALCULATOR_OPTIONS).runGenerator(); // 6815ms 841 bytes
				//new CodeLineMWEGenerator(Constants.CALCULATOR_OPTIONS_MULTI).runGenerator(); // 8945ms 726 bytes
				new ASTMWEGenerator(Constants.CALCULATOR_OPTIONS).runGenerator(); // 6122ms 718 bytes

				//new SingleCharacterMWEGenerator(Constants.FIBONACCI_OPTIONS).runGenerator(); // Timed out
				//new CodeLineMWEGenerator(Constants.FIBONACCI_OPTIONS).runGenerator(); // 3905ms 943 bytes
				//new CodeLineMWEGenerator(Constants.FIBONACCI_OPTIONS_MULTI).runGenerator(); // 4194ms 943 bytes
				//new ASTMWEGenerator(Constants.FIBONACCI_OPTIONS).runGenerator(); // 3225ms 863 bytes


				//new SingleCharacterMWEGenerator(Constants.SIMPLE_EXAMPLE_OPTIONS).runGenerator();
				//new CodeLineMWEGenerator(Constants.SIMPLE_EXAMPLE_OPTIONS).runGenerator();
				//new CodeLineMWEGenerator(Constants.SIMPLE_EXAMPLE_OPTIONS_MULTI).runGenerator();
				//new ASTMWEGenerator(Constants.SIMPLE_EXAMPLE_OPTIONS).runGenerator();
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
