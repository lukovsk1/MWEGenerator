package testexecutor.codeline;

import slice.CodeLineSlice;
import slice.ICodeSlice;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/*
    A simple extractor that considers each line as a separate slice
 */
public class CodeLineTestExecutor extends ATestExecutor {

	public CodeLineTestExecutor(CodeLineTestExecutorOptions options) {
		super(options);
	}


	@Override
	protected CodeLineTestExecutorOptions getOptions() {
		return (CodeLineTestExecutorOptions) super.getOptions();
	}

	@Override
	public List<ICodeSlice> extractSlices() {
		File sourceFolder = getSourceFolder(getOptions().getModulePath());
		List<Path> filePaths;
		try (Stream<Path> stream = Files.walk(Path.of(sourceFolder.getPath()))) {
			filePaths = stream.filter(Files::isRegularFile).toList();

		} catch (IOException e) {
			throw new ExtractorException("Unable to list files in folder" + sourceFolder.toPath(), e);
		}

		List<ICodeSlice> slices = new ArrayList<>();

		int sliceNr = 0;
		for (Path filePath : filePaths) {
			try {
				String relativeFileName = filePath.toString().substring(sourceFolder.toString().length());
				List<String> lines = Files.readAllLines(filePath);
				for (int i = 0; i < lines.size(); i++) {
					slices.add(new CodeLineSlice(relativeFileName, lines.get(i), i, sliceNr++));
				}

			} catch (IOException e) {
				throw new ExtractorException("Unable to read lines from file " + filePath, e);
			}
		}

		return slices;
	}

	@Override
	protected Map<String, String> mapSlicesToFiles(List<ICodeSlice> slices) {
		Map<String, String> files = new HashMap<>();
		String fileName = null;
		StringBuilder sb = null;
		for (ICodeSlice sl : slices) {
			CodeLineSlice slice = (CodeLineSlice) sl;
			if (!slice.getPath().equals(fileName)) {
				if (sb != null) {
					files.put(fileName, sb.toString());
				}
				fileName = slice.getPath();
				sb = new StringBuilder();
			}
			sb.append(slice.getCodeLine());
			sb.append("\n");
		}
		if (sb != null) {
			files.put(fileName, sb.toString());
		}
		return files;
	}
}
