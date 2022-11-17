package testexecutor.ast;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import slice.ASTCodeSlice;
import slice.CodeLineSlice;
import slice.ICodeSlice;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;
import testexecutor.TestingException;
import utility.FileUtility;
import utility.JavaParserUtility;
import utility.JavaParserUtility.Token;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/*
    A simple extractor that considers each line as a separate slice
 */
public class ASTTestExecutor extends ATestExecutor {

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

		AtomicInteger sliceNr = new AtomicInteger();
		for (Path filePath : filePaths) {
			try {
				String relativeFileName = filePath.toString().substring(sourceFolder.toString().length());
				String code = FileUtility.readTextFile(filePath);
				CompilationUnit javaAST = JavaParserUtility.parse(code, true);
				List<Token> tokensToAST = JavaParserUtility.tokensToAST(code, javaAST);
				slices.addAll(transformToSlices(tokensToAST, relativeFileName, sliceNr));
			} catch (IOException | InvalidInputException e) {
				throw new ExtractorException("Unable to create slices from file " + filePath, e);
			}
		}

		return slices;
	}

	private List<ASTCodeSlice> transformToSlices(List<Token> fromTokens, String relativeFileName, AtomicInteger sliceNr) {
		var tokens = fromTokens.stream()
				.sorted(Comparator.comparing(t -> t.start))
				.toList();

		// Combine all tokens that belong to the same AST node:
		Map<ASTNode, ASTCodeSlice> astNodeToSlice = new HashMap<>();
		for (Token token : tokens) {
			ASTCodeSlice slice = astNodeToSlice.get(token.node);
			if (slice == null) {
				slice = new ASTCodeSlice(relativeFileName, sliceNr.getAndIncrement());
				astNodeToSlice.put(token.node, slice);
			}
			slice.addToken(token);
		}

		var slices = new ArrayList<>(astNodeToSlice.values());
		calculateDependencies(slices);
		return slices;
	}

	private void calculateDependencies(List<ASTCodeSlice> slices) {
		// slides and their tokens are ordered by start
		for (int i = 0; i < slices.size(); i++) {
			ASTCodeSlice slice = slices.get(i);
			int start = slice.getStart();
			int end = slice.getEnd();
			for (int j = i + 1; j < slices.size(); j++) {
				ASTCodeSlice otherSlice = slices.get(j);
				if (otherSlice.getStart() > start) {
					if (otherSlice.getEnd() < end) {
						// TODO add dependency
					}
				} else {
					break;
				}
			}
		}
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
