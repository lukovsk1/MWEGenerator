package testexecutor.codeline;

import fragment.CodeLineFragment;
import fragment.ICodeFragment;
import org.apache.commons.io.FilenameUtils;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    A simple executor that considers each line as a separate fragment
 */
public class CodeLineTestExecutor extends ATestExecutor {

	public CodeLineTestExecutor(TestExecutorOptions options) {
		super(options);
	}

	@Override
	public List<ICodeFragment> extractFragments() {
		File sourceFolder = getSourceFolder(getTestSourcePath(), getOptions().getSourceFolderPath());
		List<Path> filePaths;
		String unitTestFolderPath = getTestSourcePath().toString() + "\\" + getOptions().getUnitTestFolderPath();
		try (Stream<Path> stream = Files.walk(FileSystems.getDefault().getPath(sourceFolder.getPath()))) {
			filePaths = stream
					.filter(file -> Files.isRegularFile(file)
							&& isExcludedFile(file)
							&& "java".equals(FilenameUtils.getExtension(file.toString()))
							&& !file.toString().startsWith(unitTestFolderPath))
					.collect(Collectors.toList());

		} catch (IOException e) {
			throw new ExtractorException("Unable to list files in folder" + sourceFolder.toPath(), e);
		}

		List<ICodeFragment> fragments = new ArrayList<>();

		int fragmentNr = 0;
		for (Path filePath : filePaths) {
			try {
				String relativeFileName = filePath.toString().substring(sourceFolder.toString().length());
				List<String> lines = Files.readAllLines(filePath);
				for (int i = 0; i < lines.size(); i++) {
					fragments.add(new CodeLineFragment(relativeFileName, lines.get(i), i, fragmentNr++));
				}

			} catch (IOException e) {
				throw new ExtractorException("Unable to read lines from file " + filePath, e);
			}
		}

		return fragments;
	}

	@Override
	protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
		Map<String, String> files = new HashMap<>();
		String fileName = null;
		StringBuilder sb = null;
		for (ICodeFragment fr : fragments) {
			CodeLineFragment fragment = (CodeLineFragment) fr;
			if (!fragment.getPath().equals(fileName)) {
				if (sb != null) {
					files.put(fileName, sb.toString());
				}
				fileName = fragment.getPath();
				sb = new StringBuilder();
			}
			sb.append(fragment.getCodeLine());
			sb.append("\n");
		}
		if (sb != null) {
			files.put(fileName, sb.toString());
		}
		return files;
	}
}
