package utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Scanner;

public class FileUtility {

	public static String readTextFile(Path path) throws FileNotFoundException {
		StringBuilder text = new StringBuilder();

		try (Scanner scanner = new Scanner(new FileInputStream(path.toFile()))) {
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine()).append("\n");
			}
		}

		return text.toString();
	}

}
