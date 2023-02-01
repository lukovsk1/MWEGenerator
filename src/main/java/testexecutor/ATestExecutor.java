package testexecutor;

import compiler.InMemoryJavaCompiler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.mdkt.compiler.CompilationException;
import fragment.ICodeFragment;
import utility.CollectionsUtility;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ATestExecutor implements ITestExecutor {


	private final TestExecutorOptions m_options;

	// in memory java compiler
	private InMemoryJavaCompiler m_compiler;
	private AtomicBoolean m_firstRun = new AtomicBoolean(true);
	private Set<String> m_unitTestFileNames = new HashSet<>();

	protected ATestExecutor(TestExecutorOptions options) {
		m_options = options;

		System.out.println("Create executor of class " + this.getClass().getName() + " with compilation type " + options.getCompilationType());
	}

	protected TestExecutorOptions getOptions() {
		return m_options;
	}

	protected void writeConfigurationToTestingFolder(List<ICodeFragment> fragments, Path folderPath) throws IOException {
		File testSourceFolder = getSourceFolder(folderPath, getOptions().getSourceFolderPath());
		for (Map.Entry<String, String> file : mapFragmentsToFiles(fragments).entrySet()) {
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

	protected abstract Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments);

	protected File getSourceFolder(Path modulePath, String sourceFolderPath) {
		File moduleFolder = new File(modulePath.toString());
		if (!moduleFolder.exists() || !moduleFolder.isDirectory()) {
			throw new ExtractorException("Invalid module path " + modulePath);
		}
		File sourceFolder = new File(moduleFolder.getPath() + "/" + sourceFolderPath);
		if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
			throw new ExtractorException("Missing source folder " + sourceFolder);
		}
		return sourceFolder;
	}

	protected Path getTestFolderPath() {
		String dir = System.getProperty("user.dir");
		return FileSystems.getDefault().getPath(dir + "/testingfolder");
	}

	protected Path getTestSourcePath() {
		String dir = System.getProperty("user.dir");
		return FileSystems.getDefault().getPath(dir + "/testingsource");
	}

	protected Path getTestBuildPath() {
		String dir = System.getProperty("user.dir");
		return FileSystems.getDefault().getPath(dir + "/testingbuild");
	}

	protected Path getTestOutputPath() {
		String dir = System.getProperty("user.dir");
		return FileSystems.getDefault().getPath(dir + "/testingoutput");
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

	protected void copyFolderStructure(Path src, Path dest, boolean includeJavaFiles) throws IOException {
		try (Stream<Path> stream = Files.walk(src)) {
			stream.filter(path -> includeJavaFiles || !Files.isRegularFile(path) || !"java".equals(FilenameUtils.getExtension(path.toString())))
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
	public ETestResult test(List<ICodeFragment> fragments) {
		switch (getOptions().getCompilationType().toString()) {
			case "IN_MEMORY":
				return testInMemory(fragments);
			case "COMMAND_LINE":
				return testWithCommandLine(fragments);
			default:
				throw new TestingException("Unknown compilation type");
		}
	}

	public void recreateCode(List<ICodeFragment> fragments) {
		Path testOutputPath = getTestOutputPath();
		try {
			deleteFolder(testOutputPath);
			copyFolderStructure(getTestSourcePath(), testOutputPath, false);

			// Copy unit test file
			String unitTestFolderPath = getOptions().getUnitTestFolderPath();
			Path unitTestPath = FileSystems.getDefault().getPath(testOutputPath + "\\" + unitTestFolderPath);
			deleteFolder(unitTestPath);
			copyFolderStructure(FileSystems.getDefault().getPath(getTestSourcePath() + "\\" + unitTestFolderPath), unitTestPath, true);
		} catch (IOException e) {
			throw new TestingException("Unable to copy module", e);
		}

		// write files to output folder
		try {
			writeConfigurationToTestingFolder(fragments, testOutputPath);
		} catch (IOException e) {
			throw new TestingException("Unable to write configuration to testing folder", e);
		}
	}

	@Override
	public void changeSourceToOutputFolder() {
		getOptions().withModulePath(getTestOutputPath().toString());
	}

	protected InMemoryJavaCompiler getCompiler() {
		if(m_compiler == null) {
			m_compiler = compiler.InMemoryJavaCompiler
				.newInstance()
				.ignoreWarnings();
		}
		return m_compiler;
	}

	protected ETestResult testInMemory(List<ICodeFragment> fragments) {
		InMemoryJavaCompiler compiler = getCompiler();
		String[] unitTestName = getOptions().getUnitTestMethod().split("#");

		try {
			Path unitTestFolder = FileSystems.getDefault().getPath(getTestSourcePath().toString() + "\\" + getOptions().getUnitTestFolderPath());
			if(m_firstRun.compareAndSet(true, false)) {
				Files.walk(unitTestFolder)
						.filter(path -> Files.isRegularFile(path) && "java".equals(FilenameUtils.getExtension(path.toString())))
						.forEach(path -> {
							try {
								String className = path.toString().substring(unitTestFolder.toString().length() + 1, path.toString().length() - 5).replaceAll("\\\\", ".");
								compiler.addSource(className, new String(Files.readAllBytes(path)));
								m_unitTestFileNames.add(className);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						});
			}
			Map<String, String> fragmentsToFiles = mapFragmentsToFiles(fragments);
			compiler.pruneSources(CollectionsUtility.union(m_unitTestFileNames, fragmentsToFiles.keySet().stream()
					.map(this::fileNameToClassName)
					.collect(Collectors.toList())));
			for (Map.Entry<String, String> file : fragmentsToFiles.entrySet()) {
				compiler.addSource(fileNameToClassName(file.getKey()), file.getValue());
			}
		} catch (Exception e) {
			throw new TestingException("Error while adding files", e);
		}
		Map<String, Class<?>> classes;

		try {
			classes = compiler.compileAll();
		} catch (CompilationException e) {
			if(m_options.isLogCompilationErrors()) {
				System.out.println("Compilation error: " + e);
			}
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

	protected ETestResult testWithCommandLine(List<ICodeFragment> fragments) {
		Path testSourcePath = getTestSourcePath();
		Path testFolderPath = getTestFolderPath();
		Path testBuildPath = getTestBuildPath();
		try {
			deleteFolder(testFolderPath);
			deleteFolder(testBuildPath);
			copyFolderStructure(testSourcePath, testFolderPath, false);

			// Copy unit test folder
			String unitTestFolderPath = getOptions().getUnitTestFolderPath();
			Path unitTestPath = FileSystems.getDefault().getPath(testFolderPath + "\\" + unitTestFolderPath);
			deleteFolder(unitTestPath);
			copyFolderStructure(FileSystems.getDefault().getPath(testSourcePath + "\\" + unitTestFolderPath), unitTestPath, true);
		} catch (IOException e) {
			throw new TestingException("Unable to copy module", e);
		}

		// write files to testing folder
		try {
			writeConfigurationToTestingFolder(fragments, getTestFolderPath());
		} catch (IOException e) {
			throw new TestingException("Unable to write configuration to testing folder", e);
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
					if (error.contains(getOptions().getExpectedResult())) {
						return ETestResult.FAILED;
					} else {
						return ETestResult.ERROR_RUNTIME;
					}
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

	public void initialize() {
		// copy source folder
		Path testSourcePath = getTestSourcePath();
		try {
			deleteFolder(testSourcePath);
			copyFolderStructure(FileSystems.getDefault().getPath(getOptions().getModulePath()), testSourcePath, true);
		} catch (IOException e) {
			throw new TestingException("Unable to copy module", e);
		}
	}
}
