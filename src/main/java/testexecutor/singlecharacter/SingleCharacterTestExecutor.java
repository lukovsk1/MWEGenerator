package testexecutor.singlecharacter;

import org.apache.commons.io.FilenameUtils;
import fragment.ICodeFragment;
import fragment.SingleCharacterCodeFragment;
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
    A simple test executor that considers each character as a separate fragment
 */
public class SingleCharacterTestExecutor extends ATestExecutor {

	public SingleCharacterTestExecutor(TestExecutorOptions options) {
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
				byte[] file = Files.readAllBytes(filePath);
				for (byte b : file) {
					fragments.add(new SingleCharacterCodeFragment(relativeFileName, b, fragmentNr++));
				}

			} catch (IOException e) {
				throw new ExtractorException("Unable to read characters from file " + filePath, e);
			}
		}

		return fragments;
	}

	@Override
	protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
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
		for (ICodeFragment fr : fragments) {
			SingleCharacterCodeFragment fragment = (SingleCharacterCodeFragment) fr;
			if (!fragment.getPath().equals(fileName)) {
				byteWriter.accept(bytes, fileName);
				fileName = fragment.getPath();
				bytes = new ArrayList<>();
			}
			bytes.add(fragment.getContent());
		}
		byteWriter.accept(bytes, fileName);
		return files;
	}
}
