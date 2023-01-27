package testexecutor.singlecharacter;

import org.apache.commons.io.FilenameUtils;
import slice.ICodeSlice;
import slice.SingleCharacterCodeSlice;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;
import testexecutor.TestExecutorOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    A simple extractor that considers each line as a separate slice
 */
public class SingleCharacterTestExecutor extends ATestExecutor {

	public SingleCharacterTestExecutor(TestExecutorOptions options) {
		super(options);
	}

	@Override
	public List<ICodeSlice> extractSlices() {
		File sourceFolder = getSourceFolder(getTestSourcePath(), getOptions().getSourceFolderPath());
		List<Path> filePaths;
		String unitTestFolderPath = getTestSourcePath().toString() + "\\" + getOptions().getUnitTestFolderPath();
		try (Stream<Path> stream = Files.walk(FileSystems.getDefault().getPath(sourceFolder.getPath()))) {
			filePaths = stream
					.filter(file -> Files.isRegularFile(file)
							&& "java".equals(FilenameUtils.getExtension(file.toString()))
							&& !file.toString().startsWith(unitTestFolderPath))
					.collect(Collectors.toList());

		} catch (IOException e) {
			throw new ExtractorException("Unable to list files in folder" + sourceFolder.toPath(), e);
		}

		List<ICodeSlice> slices = new ArrayList<>();

		int sliceNr = 0;
		for (Path filePath : filePaths) {
			try {
				String relativeFileName = filePath.toString().substring(sourceFolder.toString().length());
				byte[] file = Files.readAllBytes(filePath);
				for (byte b : file) {
					slices.add(new SingleCharacterCodeSlice(relativeFileName, b, sliceNr++));
				}

			} catch (IOException e) {
				throw new ExtractorException("Unable to read characters from file " + filePath, e);
			}
		}

		return slices;
	}

	@Override
	protected Map<String, String> mapSlicesToFiles(List<ICodeSlice> slices) {
		Map<String, String> files = new HashMap<>();
		String fileName = null;
		List<Byte> bytes = new ArrayList<>();
		BiConsumer<List<Byte>, String> byteWriter = (byteList, fn) -> {
			if (byteList.size() > 0) {
				byte[] byteArray = new byte[byteList.size()];
				for (int i = 0; i < byteList.size(); i++) {
					byteArray[i] = byteList.get(i);
				}
				files.put(fn, new String(byteArray, StandardCharsets.UTF_8));
			}
		};
		for (ICodeSlice sl : slices) {
			SingleCharacterCodeSlice slice = (SingleCharacterCodeSlice) sl;
			if (!slice.getPath().equals(fileName)) {
				byteWriter.accept(bytes, fileName);
				fileName = slice.getPath();
				bytes = new ArrayList<>();
			}
			bytes.add(slice.getContent());
		}
		byteWriter.accept(bytes, fileName);
		return files;
	}
}
