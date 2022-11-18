package testexecutor;

import org.apache.commons.io.IOUtils;
import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.InMemoryJavaCompiler;
import slice.ICodeSlice;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ATestExecutor implements ITestExecutor {

	private final ATestExecutorOptions m_options;

	protected ATestExecutor(ATestExecutorOptions options) {
		m_options = options;

		System.out.println("Create executor of class " + this.getClass().getName() + " with comilation type " + options.getCompilationType());
	}

	protected ATestExecutorOptions getOptions() {
		return m_options;
	}

	protected void writeSlicesToTestingFolder(List<ICodeSlice> slices, Path folderPath) throws IOException {
		File testSourceFolder = getSourceFolder(folderPath.toString());
		for (Map.Entry<String, String> file : mapSlicesToFiles(slices).entrySet()) {
			String fileName = file.getKey();
			File newFile = new File(testSourceFolder.getPath() + fileName);
			if (!newFile.createNewFile()) {
				throw new TestingException("Unable to recreate file " + fileName);
			}
			FileOutputStream fos = new FileOutputStream(newFile);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
			writer.write(file.getValue());
			writer.close();
		}
	}

	protected abstract Map<String, String> mapSlicesToFiles(List<ICodeSlice> slices);

	protected File getSourceFolder(String modulePath) {
		File moduleFolder = new File(modulePath);
		if (!moduleFolder.exists() || !moduleFolder.isDirectory()) {
			throw new ExtractorException("Invalid module path " + modulePath);
		}
		File sourceFolder = new File(moduleFolder.getPath() + "/src");
		if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
			throw new ExtractorException("Missing source folder in module " + modulePath);
		}
		return sourceFolder;
	}

	protected Path getTestFolderPath() {
		String dir = System.getProperty("user.dir");
		return Path.of(dir + "/testingfolder");
	}

	protected Path getTestSourcePath() {
		String dir = System.getProperty("user.dir");
		return Path.of(dir + "/testingsource");
	}

	protected Path getTestBuildPath() {
		String dir = System.getProperty("user.dir");
		return Path.of(dir + "/testingbuild");
	}

	protected Path getTestOutputPath() {
		String dir = System.getProperty("user.dir");
		return Path.of(dir + "/testingoutput");
	}

	protected void deleteFolder(Path folder) throws IOException {
		if (!Files.exists(folder)) {
			return;
		}
		Files.walk(folder)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
	}

	protected void copyFolderStructure(Path src, Path dest, boolean includeFiles) throws IOException {
		try (Stream<Path> stream = Files.walk(src)) {
			stream.filter(path -> includeFiles || !Files.isRegularFile(path))
					.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
		}
	}

	protected void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	protected String fileNameToClassName(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return null;
		}

		if (fileName.matches("^[.\\\\/].+")) {
			fileName = fileName.substring(1);
		}
		if (fileName.endsWith(".java")) {
			fileName = fileName.substring(0, fileName.length() - 5);
		}
		return fileName.replaceAll("[\\\\/]", ".");
	}

	@Override
	public ETestResult test(List<ICodeSlice> slices) {
		switch (getOptions().getCompilationType()) {
			case IN_MEMORY -> {
				return testInMemory(slices);
			}
			case COMMAND_LINE -> {
				return testWithCommandLine(slices);
			}
			default -> throw new TestingException("Unknown compilation type");
		}
	}

	public void recreateCode(List<ICodeSlice> slices) {
		Path testOutputPath = getTestOutputPath();
		try {
			deleteFolder(testOutputPath);
			copyFolderStructure(getTestSourcePath(), testOutputPath, false);
		} catch (IOException e) {
			throw new TestingException("Unable to copy module", e);
		}
		// Copy unit test file
		String unitTestFilePath = getOptions().getUnitTestFilePath();
		if (!unitTestFilePath.endsWith(".java")) {
			unitTestFilePath += ".java";
		}
		copy(Path.of(getTestSourcePath() + "\\" + unitTestFilePath),
				Path.of(testOutputPath + "\\" + unitTestFilePath));

		// write files to output folder
		try {
			writeSlicesToTestingFolder(slices, testOutputPath);
		} catch (IOException e) {
			throw new TestingException("Unable to write slices to testing folder", e);
		}
	}

	@Override
	public void changeSourceToOutputFolder() {
		getOptions().withModulePath(getTestOutputPath().toString());
	}

	protected ETestResult testInMemory(List<ICodeSlice> slices) {
		// copy source folder
		Path testSourcePath = getTestSourcePath();
		try {
			deleteFolder(testSourcePath);
			copyFolderStructure(Path.of(getOptions().getModulePath()), testSourcePath, true);
		} catch (IOException e) {
			throw new TestingException("Unable to copy module", e);
		}
		// Copy unit test file to source folder
		String unitTestFilePath = getOptions().getUnitTestFilePath();
		if (!unitTestFilePath.endsWith(".java")) {
			unitTestFilePath += ".java";
		}
		copy(Path.of(getOptions().getModulePath() + "\\" + unitTestFilePath),
				Path.of(testSourcePath + "\\" + unitTestFilePath));

		InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();

		String[] unitTestName = getOptions().getUnitTestMethod().split("#");

		try {
			String unitTestFile = Files.readString(Path.of(getOptions().getModulePath() + "\\" + unitTestFilePath));
			compiler.addSource(unitTestName[0], unitTestFile);
			for (Map.Entry<String, String> file : mapSlicesToFiles(slices).entrySet()) {
				compiler.addSource(fileNameToClassName(file.getKey()), file.getValue());
			}
		} catch (Exception e) {
			throw new TestingException("Error while adding files", e);
		}
		Map<String, Class<?>> classes;

		try {
			classes = compiler.compileAll();
		} catch (CompilationException e) {
			return ETestResult.ERROR_COMPILATION;
		} catch (Exception e) {
			throw new TestingException("Error during compilation", e);
		}


		// execute unit test
		Class<?> unitTestClass = classes.get(unitTestName[0]);
		try {
			Constructor<?> constructor = unitTestClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			Object unitTest = constructor.newInstance();

			Method testingMethod = unitTestClass.getDeclaredMethod(unitTestName[1]);
			testingMethod.setAccessible(true);

			try {
				testingMethod.invoke(unitTest);
				return ETestResult.OK;
			} catch (Exception ex) {
				if (ex.getCause().toString() != null && ex.getCause().toString().contains(getOptions().getExpectedResult())) {
					return ETestResult.FAILED;
				}
			}
		} catch (Exception e) {
			throw new TestingException("Error during test execution", e);
		}


		return ETestResult.ERROR_RUNTIME;
	}

	protected ETestResult testWithCommandLine(List<ICodeSlice> slices) {
		Path testSourcePath = getTestSourcePath();
		Path testFolderPath = getTestFolderPath();
		Path testBuildPath = getTestBuildPath();
		try {
			deleteFolder(testFolderPath);
			deleteFolder(testBuildPath);
			deleteFolder(testSourcePath);
			copyFolderStructure(Path.of(getOptions().getModulePath()), testSourcePath, true);
			copyFolderStructure(Path.of(getOptions().getModulePath()), testFolderPath, false);
		} catch (IOException e) {
			throw new TestingException("Unable to copy module", e);
		}
		// Copy unit test file
		String unitTestFilePath = getOptions().getUnitTestFilePath();
		if (!unitTestFilePath.endsWith(".java")) {
			unitTestFilePath += ".java";
		}
		copy(Path.of(getOptions().getModulePath() + "\\" + unitTestFilePath),
				Path.of(testFolderPath + "\\" + unitTestFilePath));
		copy(Path.of(getOptions().getModulePath() + "\\" + unitTestFilePath),
				Path.of(testSourcePath + "\\" + unitTestFilePath));

		// write files to testing folder
		try {
			writeSlicesToTestingFolder(slices, getTestFolderPath());
		} catch (IOException e) {
			throw new TestingException("Unable to write slices to testing folder", e);
		}

		// compile java classes for testing
		String classPath = System.getProperty("java.class.path");
		classPath = Arrays.stream(classPath.split(";")).filter(
				dependency -> dependency.endsWith(".jar") && !dependency.contains("JetBrains")
		).collect(Collectors.joining(";"));

		String javaHome = System.getProperty("java.home");

		final List<String> commands = new ArrayList<>();
		commands.add("cmd.exe");
		commands.add("/C");
		commands.add("cd testingfolder");
		commands.add("& dir /s /B *.java > testingsources.txt"); // write paths of all java files to sources file
		commands.add("& findstr /v \"^$\" testingsources.txt > temp.txt & move /y temp.txt testingsources.txt >nul "); // remove empty lines from sources file
		commands.add("& " + javaHome + "/bin/javac -d ../testingbuild -cp " + classPath + " @testingsources.txt"); // compile all java files

		ProcessBuilder pb = new ProcessBuilder(commands);
		Process p;
		try {
			p = pb.start();
			if (!p.waitFor(1, TimeUnit.MINUTES)) {
				throw new TestingException("Timed out while compiling java code");
			}
			if (p.exitValue() > 0) {
				return ETestResult.ERROR_COMPILATION;
			}
		} catch (IOException | InterruptedException e) {
			throw new TestingException("Unexpected error while compiling java code", e);
		}

		// run unit test
		commands.clear();
		commands.add("cmd.exe");
		commands.add("/C");
		commands.add(javaHome + "/bin/java -Dfile.encoding=UTF-8 " +
				"-classpath " + testBuildPath + ";" + classPath +
				" org.junit.platform.console.ConsoleLauncher --select-method " + getOptions().getUnitTestMethod());

		ProcessBuilder pb2 = new ProcessBuilder(commands);
		Process p2;
		try {
			p2 = pb2.start();
			if (!p2.waitFor(1, TimeUnit.MINUTES)) {
				throw new TestingException("Timed out while running testing code");
			}
			if (p2.exitValue() > 0) {
				String input = IOUtils.toString(p2.getInputStream(), StandardCharsets.UTF_8);
				String error = IOUtils.toString(p2.getErrorStream(), StandardCharsets.UTF_8);
				if (error != null && error.length() > 0) {
					return ETestResult.ERROR_RUNTIME;
				}
				if (input.contains(getOptions().getExpectedResult())) {
					return ETestResult.FAILED;
				}
				return ETestResult.ERROR_RUNTIME;
			}
		} catch (IOException | InterruptedException e) {
			throw new TestingException("Unexpected error while executing unit test", e);
		}

		return ETestResult.OK;
	}
}
