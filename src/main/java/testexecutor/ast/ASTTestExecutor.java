package testexecutor.ast;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import slice.CodeLineSlice;
import slice.ICodeSlice;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;
import testexecutor.TestingException;
import utility.FileUtility;
import utility.JavaParserUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
public class ASTTestExecutor extends ATestExecutor {

	public static int JAVA_LANGUAGE_SPECIFICATION = AST.getJLSLatest();


	public ASTTestExecutor(ASTTestExecutorOptions options) {
		super(options);
	}


	@Override
	protected ASTTestExecutorOptions getOptions() {
		return (ASTTestExecutorOptions) super.getOptions();
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
				String code = FileUtility.readTextFile(filePath);
				CompilationUnit javaAST = JavaParserUtility.parse(code, true);
				List<JavaParserUtility.Token> tokensToAST = JavaParserUtility.tokensToAST(code, javaAST);

	
			} catch (IOException | InvalidInputException e) {
				throw new ExtractorException("Unable to create slices from file " + filePath, e);
			}
		}

		return slices;
	}


	@Override
	protected BiConsumer<BufferedWriter, ICodeSlice> getFileWriterConsumer() {
		return (writer, slice) -> {
			try {
				writer.write(((CodeLineSlice) slice).getCodeLine());
			} catch (IOException e) {
				throw new TestingException("Error while recreating test file");
			}
		};
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
