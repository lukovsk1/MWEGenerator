package testexecutor;

import slice.CodeLineSlice;
import slice.ICodeSlice;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
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
                List<String> lines = Files.readAllLines(filePath);
                for(int i = 0; i < lines.size(); i++) {
                    slices.add(new CodeLineSlice(filePath.toString(), lines.get(i), i));
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
            copyFolderStructure(Path.of(m_options.getModulePath()), testFolderPath);
        } catch (Throwable t) {
            throw new TestingException("Unable to copy module", t);
        }

        // write files
        getSourceFolder(testFolderPath.toString());

        return ETestResult.ERROR;
    }

    private Path getTestFolderPath() {
        String dir = System.getProperty("user.dir");
        return Path.of(dir + "/ddminj");
    }

    public  void copyFolderStructure(Path src, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.filter(path -> !Files.isRegularFile(path))
                .forEach(source -> copyFolder(source, dest.resolve(src.relativize(source))));
        }
    }

    private void copyFolder(Path source, Path dest) {
        try {
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
