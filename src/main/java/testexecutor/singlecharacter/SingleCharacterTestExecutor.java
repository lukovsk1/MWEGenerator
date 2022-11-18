package testexecutor.singlecharacter;

import slice.ICodeSlice;
import slice.SingleCharacterCodeSlice;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/*
    A simple extractor that considers each line as a separate slice
 */
public class SingleCharacterTestExecutor extends ATestExecutor {

	public SingleCharacterTestExecutor(SingleCharacterTestExecutorOptions options) {
		super(options);
	}


	@Override
	protected SingleCharacterTestExecutorOptions getOptions() {
		return (SingleCharacterTestExecutorOptions) super.getOptions();
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
