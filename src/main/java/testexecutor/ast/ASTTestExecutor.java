package testexecutor.ast;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import slice.ASTCodeSlice;
import slice.ICodeSlice;
import testexecutor.ATestExecutor;
import testexecutor.ExtractorException;
import testexecutor.TestExecutorOptions;
import testexecutor.TestingException;
import utility.CollectionsUtility;
import utility.FileUtility;
import utility.JavaParserUtility;
import utility.JavaParserUtility.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    A simple extractor that considers each line as a separate slice
 */
public class ASTTestExecutor extends ATestExecutor {

	private final Set<ICodeSlice> m_fixedSlices = new HashSet<>();
	private boolean m_isRecreating = false;

	public ASTTestExecutor(TestExecutorOptions options) {
		super(options);
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
				List<Token> tokens = JavaParserUtility.tokensToAST(code, javaAST);
				slices.add(transformToSlices(javaAST, tokens, relativeFileName, sliceNr));
			} catch (IOException | InvalidInputException e) {
				throw new ExtractorException("Unable to create slices from file " + filePath, e);
			}
		}

		return slices;
	}

	private ASTCodeSlice transformToSlices(CompilationUnit javaAST, List<Token> tokens, String relativeFileName, AtomicInteger sliceNr) {
		Map<ASTNode, ASTCodeSlice> astNodeToSlice = new HashMap<>();
		ASTCodeSlice rootSlice = new ASTCodeSlice(relativeFileName, sliceNr.getAndIncrement());
		rootSlice.setLevel(0);
		// Combine all tokens that belong to the same AST node:
		for (Token token : tokens) {
			ASTCodeSlice slice = astNodeToSlice.get(token.node);
			if (slice == null) {
				slice = new ASTCodeSlice(relativeFileName, sliceNr.getAndIncrement());
				astNodeToSlice.put(token.node, slice);
				for(ASTNode additionalNode : token.additionalNodes) {
					astNodeToSlice.put(additionalNode, slice);
				}
			}
			slice.addToken(token);
		}
		calculateDependencies(javaAST.getRoot(), rootSlice, astNodeToSlice);

		return rootSlice;
	}

	private void calculateDependencies(ASTNode rootNode, ASTCodeSlice rootSlice, Map<ASTNode, ASTCodeSlice> nodesToSlices) {
		// For each node, calculate its children and its level
		// TODO: a more efficient implementation might be possible
		if (nodesToSlices.isEmpty()) {
			return;
		}
		int level = 1;
		Set<ASTNode> nodesOnParentLevel = Collections.emptySet();
		Set<ASTNode> nodesOnLevel = new HashSet<>();
		AtomicReference<ASTNode> parent = new AtomicReference<>(rootNode);
		// check if we have a slice without a calculated level
		while (true) {
			var childNodes = nodesToSlices.keySet()
					.stream()
					.filter(node -> Objects.equals(node.getParent(), parent.get()))
					.filter(node -> !Objects.equals(node, parent.get()))
					.toList();
			for (var child : childNodes) {
				ASTCodeSlice slice = nodesToSlices.get(child);
				slice.setLevel(level);
				nodesOnLevel.add(child);
				if (parent.get() != null && nodesToSlices.get(parent.get()) != null) {
					nodesToSlices.get(parent.get()).addChild(slice);
				} else {
					rootSlice.addChild(slice);
				}
			}
			// descend to next level
			if (nodesOnParentLevel.isEmpty()) {
				if (nodesOnLevel.isEmpty()) {
					break;
				}
				nodesOnParentLevel = new HashSet<>(nodesOnLevel);
				nodesOnLevel = new HashSet<>();
				level++;
			}

			// set next parent and remove it from set
			parent.set(nodesOnParentLevel.iterator().next());
			nodesOnParentLevel.remove(parent.get());
		}
		// sometimes there are middle nodes, that are not assigned to a token in a slice
		for (var unassignedEntry : nodesToSlices.entrySet()
				.stream()
				.filter(e -> e.getValue().getLevel() < 0)
				.sorted(Comparator.comparing(e -> e.getValue().getStart()))
				.toList()) {
			var slice = unassignedEntry.getValue();
			var ancestorNode = unassignedEntry.getKey().getParent();
			while (nodesToSlices.get(ancestorNode) == null) {
				ancestorNode = ancestorNode.getParent();
				if (ancestorNode == null) {
					throw new TestingException("Unable to calculate dependencies. Found unassignable node");
				}
			}
			var parentSlice = nodesToSlices.get(ancestorNode);
			parentSlice.addChild(slice);
			slice.setLevel(parentSlice.getLevel() + 1);
		}
		if (!nodesToSlices.entrySet().stream().filter(e -> e.getValue().getLevel() < 0).toList().isEmpty()) {
			throw new TestingException("Unable to calculate dependencies. Cannot fix unassigned nodes");
		}
	}

	@Override
	protected Map<String, String> mapSlicesToFiles(List<ICodeSlice> activeSlices) {
		Map<String, String> files = new HashMap<>();

		// add active slices and all their children
		Map<String, Set<ICodeSlice>> slicesByFile = activeSlices.stream()
				.map(sl -> ((ASTCodeSlice) sl))
				.flatMap(sl -> m_isRecreating ? Stream.of(sl) : CollectionsUtility.getChildrenInDeep(sl).stream())
				.map(sl -> (ICodeSlice) sl)
				.collect(Collectors.groupingBy(ICodeSlice::getPath, Collectors.mapping(sl -> sl, Collectors.toSet())));

		// add fixed slices without any children
		m_fixedSlices.forEach(sl -> {
			var f = slicesByFile.getOrDefault(sl.getPath(), new HashSet<>());
			f.add(sl);
			slicesByFile.put(sl.getPath(), f);
		});

		for (Map.Entry<String, Set<ICodeSlice>> entry : slicesByFile.entrySet()) {
			String fileName = entry.getKey();
			StringBuilder sb = new StringBuilder();
			entry.getValue().stream()
					.flatMap(sl -> ((ASTCodeSlice) sl).getTokens().stream())
					.sorted(Comparator.comparing(t -> t.start))
					.forEach(token -> sb.append(token.code));

			files.put(fileName, sb.toString());
		}
		return files;
	}

	public void addFixedSlices(List<ICodeSlice> minConfig) {
		m_fixedSlices.addAll(minConfig);
	}

	@Override
	public void recreateCode(List<ICodeSlice> slices) {
		m_isRecreating = true;
		super.recreateCode(slices);
	}
}
