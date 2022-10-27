package testexecutor;

import org.apache.commons.io.IOUtils;
import slice.CodeLineSlice;
import slice.ICodeSlice;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    A simple extractor that considers each line as a separate slice
 */
public class CodeLineTestExecutor implements ITestExecutor {

    private final CodeLineTestExecutorOptions m_options;

    public CodeLineTestExecutor(CodeLineTestExecutorOptions options) {
        m_options = options;
    }

    @Override
    public List<ICodeSlice> extractSlices() {
        File sourceFolder = getSourceFolder(m_options.getModulePath());
        List<Path> filePaths;
        try (Stream<Path> stream = Files.walk(Path.of(sourceFolder.getPath()))) {
            filePaths = stream.filter(Files::isRegularFile).toList();

        } catch (IOException e) {
            throw new ExtractorException("Unable to list files in folder" + sourceFolder.toPath(), e);
        }

        List<ICodeSlice> slices = new ArrayList<>();

        for(Path filePath : filePaths) {
            try {
                String relativeFileName = filePath.toString().substring(sourceFolder.toString().length());
                List<String> lines = Files.readAllLines(filePath);
                for(int i = 0; i < lines.size(); i++) {
                    slices.add(new CodeLineSlice(relativeFileName, lines.get(i), i));
                }

            } catch (IOException e) {
                throw new ExtractorException("Unable to read lines from file " + filePath, e);
            }
        }

        return slices;
    }

    private File getSourceFolder(String modulePath) {
        File moduleFolder = new File(modulePath);
        if(!moduleFolder.exists()|| !moduleFolder.isDirectory()) {
            throw new ExtractorException("Invalid module path " + modulePath);
        }
        File sourceFolder = new File(moduleFolder.getPath() + "/src");
        if(!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            throw new ExtractorException("Missing source folder in module " + modulePath);
        }
        return sourceFolder;
    }

    @Override
    public ETestResult test(List<ICodeSlice> slices) {
        Path testFolderPath = getTestFolderPath();
        try {
            deleteFolder(testFolderPath);
            deleteFolder(getTestBuildPath());
            copyFolderStructure(Path.of(m_options.getModulePath()), testFolderPath);
        } catch (IOException e) {
            throw new TestingException("Unable to copy module", e);
        }
        // Copy unit test file
        String unitTestFilePath = m_options.getUnitTestFilePath();
        if(!unitTestFilePath.endsWith(".java")) {
            unitTestFilePath += ".java";
        }
        copy(Path.of(m_options.getModulePath() + "\\" + unitTestFilePath),
                Path.of(testFolderPath + "\\" + unitTestFilePath));

        // write files to testing folder
        String filesForClassPath;
        try {
            filesForClassPath = writeSlicesToTestingFolder(slices, testFolderPath.toString());
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
        commands.add("& dir /s /B *.java > testingsources.txt");
        commands.add("& " + javaHome + "/bin/javac -d ../testingbuild -cp " + classPath + " @testingsources.txt");
        commands.add("& cd ../testingbuild");
        commands.add("& jar cvf TestingBuild.jar *");
        ProcessBuilder pb = new ProcessBuilder(commands);
        Process p;
        try {
            p = pb.start();p.waitFor(1, TimeUnit.MINUTES);
            if(p.exitValue() > 0) {
                return ETestResult.ERROR_COMPILATION;
            }
        }
        catch (IOException | InterruptedException e) {
            throw new TestingException("Unexpected error while compiling java code", e);
        }

        // run unit test
        commands.clear();
        commands.add("cmd.exe");
        commands.add("/C");
        commands.add(javaHome + "/bin/java -Dfile.encoding=UTF-8 " +
                "-classpath C:\\Users\\lubo9\\.m2\\repository\\org\\junit\\platform\\junit-platform-console-standalone\\1.9.1\\junit-platform-console-standalone-1.9.1.jar;C:\\dev\\workspace\\ddminj\\testingbuild;C:\\Users\\lubo9\\.m2\\repository\\org\\junit\\jupiter\\junit-jupiter\\5.8.1\\junit-jupiter-5.8.1.jar; " +
                "org.junit.platform.console.ConsoleLauncher --select-class calculator.CalculatorTest");

        ProcessBuilder pb2 = new ProcessBuilder(commands);
        Process p2;
        try {
            p2 = pb2.start();
            String input = IOUtils.toString(p2.getInputStream(), StandardCharsets.UTF_8);
            String error = IOUtils.toString(p2.getErrorStream(), StandardCharsets.UTF_8);
            p2.waitFor(1, TimeUnit.MINUTES);
            if(p2.exitValue() > 0) {
                if(error != null && error.length() > 0) {
                    return ETestResult.ERROR_RUNTIME;
                }
                if(input.contains(m_options.getExpectedResult())) {
                    return ETestResult.FAILED;
                }
                return ETestResult.ERROR_RUNTIME;
            }
        }
        catch (IOException | InterruptedException e) {
            throw new TestingException("Unexpected error while executing unit test", e);
        }

        return ETestResult.OK;
    }

    private int execute(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(new String[]{
                command
        });
        return process.waitFor();
    }

    private Path getTestFolderPath() {
        String dir = System.getProperty("user.dir");
        return Path.of(dir + "/testingfolder");
    }

    private Path getTestBuildPath() {
        String dir = System.getProperty("user.dir");
        return Path.of(dir + "/testbuild");
    }

    public void deleteFolder(Path folder) throws IOException {
        if(!Files.exists(folder)) {
            return;
        }
        Files.walk(folder)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public void copyFolderStructure(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.filter(path -> !Files.isRegularFile(path))
                .forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    private void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String writeSlicesToTestingFolder(List<ICodeSlice> slices, String testFolderPath) throws IOException {
        String filesForClassPath = "";
        File testSourceFolder = getSourceFolder(testFolderPath);
        String fileName = null;
        BufferedWriter writer = null;
        for(ICodeSlice sl : slices) {
            CodeLineSlice slice = (CodeLineSlice) sl;
            if(!slice.getPath().equals(fileName)) {
                if(writer != null) {
                    writer.close();
                }
                fileName = slice.getPath();
                filesForClassPath += fileName.split("\\.")[0] + ";";
                File newFile = new File(testSourceFolder.getPath() + fileName);
                if(!newFile.createNewFile()) {
                    throw new TestingException("Unable to recreate file " + fileName);
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                writer = new BufferedWriter(new OutputStreamWriter(fos));
            }
            writer.write(slice.getCodeLine());
            writer.newLine();
        }
        if(writer != null) {
            writer.close();
        }

        return filesForClassPath;
    }
}
