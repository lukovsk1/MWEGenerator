package testexecutor;

import org.apache.commons.io.IOUtils;
import slice.ICodeSlice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ATestExecutor implements ITestExecutor {

    abstract ATestExecutorOptions getOptions();

    abstract void writeSlicesToTestingFolder(List<ICodeSlice> slices) throws IOException;

    abstract Map<String,String> mapSlicesToFiles(List<ICodeSlice> slices);

    protected File getSourceFolder(String modulePath) {
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

    protected Path getTestFolderPath() {
        String dir = System.getProperty("user.dir");
        return Path.of(dir + "/testingfolder");
    }

    protected Path getTestBuildPath() {
        String dir = System.getProperty("user.dir");
        return Path.of(dir + "/testingbuild");
    }

    protected void deleteFolder(Path folder) throws IOException {
        if(!Files.exists(folder)) {
            return;
        }
        Files.walk(folder)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    protected void copyFolderStructure(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.filter(path -> !Files.isRegularFile(path))
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

    @Override
    public ETestResult test(List<ICodeSlice> slices) {
        switch (getOptions().getCompilationType()) {
            case IN_MEMORY -> {
                return null;
            }
            case COMMAND_LINE -> {
                return testWithCommandLine(slices);
            }
            default ->
                throw new TestingException("Unknown compilation type");
        }
    }

    protected ETestResult testInMemory(List<ICodeSlice> slices) {


        return ETestResult.ERROR_COMPILATION;
    }

    protected ETestResult testWithCommandLine(List<ICodeSlice> slices) {
        Path testFolderPath = getTestFolderPath();
        Path testBuildPath = getTestBuildPath();
        try {
            deleteFolder(testFolderPath);
            deleteFolder(testBuildPath);
            copyFolderStructure(Path.of(getOptions().getModulePath()), testFolderPath);
        } catch (IOException e) {
            throw new TestingException("Unable to copy module", e);
        }
        // Copy unit test file
        String unitTestFilePath = getOptions().getUnitTestFilePath();
        if(!unitTestFilePath.endsWith(".java")) {
            unitTestFilePath += ".java";
        }
        copy(Path.of(getOptions().getModulePath() + "\\" + unitTestFilePath),
                Path.of(testFolderPath + "\\" + unitTestFilePath));

        // write files to testing folder
        try {
            writeSlicesToTestingFolder(slices);
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
            if(!p.waitFor(1, TimeUnit.MINUTES)) {
                throw new TestingException("Timed out while compiling java code");
            }
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
                "-classpath " + testBuildPath + ";" + classPath +
                " org.junit.platform.console.ConsoleLauncher --select-method " + getOptions().getUnitTestMethod());

        ProcessBuilder pb2 = new ProcessBuilder(commands);
        Process p2;
        try {
            p2 = pb2.start();
            if(!p2.waitFor(1, TimeUnit.MINUTES)) {
                throw new TestingException("Timed out while running testing code");
            }
            if(p2.exitValue() > 0) {
                String input = IOUtils.toString(p2.getInputStream(), StandardCharsets.UTF_8);
                String error = IOUtils.toString(p2.getErrorStream(), StandardCharsets.UTF_8);
                if(error != null && error.length() > 0) {
                    return ETestResult.ERROR_RUNTIME;
                }
                if(input.contains(getOptions().getExpectedResult())) {
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
}
