package utility;

import compiler.InMemoryJavaCompiler;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;
import java.util.stream.Stream;

public class FileUtility {

	public static String readTextFile(Path path) throws IOException {
		StringBuilder text = new StringBuilder();

		try (Scanner scanner = new Scanner(Files.newInputStream(path.toFile().toPath()))) {
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine()).append("\n");
			}
		}

		return text.toString();
	}

	public static void deleteFolder(Path folder) throws IOException {
		if (!Files.exists(folder)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(folder)) {
			walk.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

	public static void addJavaFilesToCompiler(InMemoryJavaCompiler compiler, Path folderPath) throws IOException {
		try (Stream<Path> walk = Files.walk(folderPath)) {
			walk.filter(path -> Files.isRegularFile(path) && "java".equals(FilenameUtils.getExtension(path.toString())))
					.forEach(path -> {
						try {
							String className = path.toString().substring(folderPath.toString().length() + 1, path.toString().length() - 5).replaceAll("\\\\", ".");
							compiler.addSource(className, new String(Files.readAllBytes(path)));
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					});
		}
	}

}
