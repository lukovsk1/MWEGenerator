package testexecutor.ast;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import fragment.ASTCodeFragment;
import fragment.ICodeFragment;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
    An executor that extracts the hierarchical code fragments from the AST of the source code
 */
public class ASTTestExecutor extends ATestExecutor {

	private final Set<ICodeFragment> m_fixedFragments = new HashSet<>();
	private boolean m_isRecreating = false;

	public ASTTestExecutor(TestExecutorOptions options) {
		super(options);
	}

	@Override
	public List<ICodeFragment> extractFragments() {
		File sourceFolder = getSourceFolder(getTestSourcePath(), getOptions().getSourceFolderPath());
		List<Path> filePaths;
		try (Stream<Path> stream = Files.walk(FileSystems.getDefault().getPath(sourceFolder.getPath()))) {
			filePaths = stream.filter(Files::isRegularFile).collect(Collectors.toList());

		} catch (IOException e) {
			throw new ExtractorException("Unable to list files in folder" + sourceFolder.toPath(), e);
		}

		List<ICodeFragment> fragments = new ArrayList<>();

		AtomicInteger fragmentNr = new AtomicInteger();
		String unitTestFolderPath = getTestSourcePath().toString() + "\\" + getOptions().getUnitTestFolderPath();
		for (Path filePath : filePaths) {
			if(!"java".equals(FilenameUtils.getExtension(filePath.toString()))
			 || filePath.toString().startsWith(unitTestFolderPath) ) {
				// skip non-java and unit test files
				continue;
			}
			try {
				String relativeFileName = filePath.toString().substring(sourceFolder.toString().length());
 				String code = FileUtility.readTextFile(filePath);
				CompilationUnit javaAST = JavaParserUtility.parse(code, true);
				List<Token> tokens = JavaParserUtility.tokensToAST(code, javaAST);
				fragments.add(transformToFragements(javaAST, tokens, relativeFileName, fragmentNr));
			} catch (IOException | InvalidInputException e) {
				throw new ExtractorException("Unable to create fragments from file " + filePath, e);
			}
		}

		return fragments;
	}

	private ASTCodeFragment transformToFragements(CompilationUnit javaAST, List<Token> tokens, String relativeFileName, AtomicInteger fragmentNr) {
		Map<ASTNode, ASTCodeFragment> astNodeToFragment = new HashMap<>();
		ASTCodeFragment rootFragment = new ASTCodeFragment(relativeFileName, fragmentNr.getAndIncrement());
		rootFragment.setLevel(0);
		astNodeToFragment.put(javaAST, rootFragment);
		// Combine all tokens that belong to the same AST node:
		for (Token token : tokens) {
			ASTCodeFragment fragment = astNodeToFragment.get(token.node);
			if (fragment == null) {
				fragment = new ASTCodeFragment(relativeFileName, fragmentNr.getAndIncrement());
				astNodeToFragment.put(token.node, fragment);
				for (ASTNode additionalNode : token.additionalNodes) {
					astNodeToFragment.put(additionalNode, fragment);
				}
			}
			fragment.addToken(token);
		}
		calculateDependencies(javaAST.getRoot(), rootFragment, astNodeToFragment);

		return rootFragment;
	}

	private void calculateDependencies(ASTNode rootNode, ASTCodeFragment rootFragment, Map<ASTNode, ASTCodeFragment> nodesToFragments) {
		// For each node, calculate its children and its level
		if (nodesToFragments.isEmpty()) {
			return;
		}
		int level = 1;
		Set<ASTNode> nodesOnParentLevel = Collections.emptySet();
		Set<ASTNode> nodesOnLevel = new HashSet<>();
		AtomicReference<ASTNode> parent = new AtomicReference<>(rootNode);
		// check if we have a fragment without a calculated level
		while (true) {
			List<ASTNode> childNodes = nodesToFragments.keySet()
					.stream()
					.filter(node -> Objects.equals(node.getParent(), parent.get()))
					.filter(node -> !Objects.equals(node, parent.get()))
					.collect(Collectors.toList());
			for (ASTNode child : childNodes) {
				ASTCodeFragment fragment = nodesToFragments.get(child);
				fragment.setLevel(level);
				nodesOnLevel.add(child);
				if (parent.get() != null && nodesToFragments.get(parent.get()) != null) {
					nodesToFragments.get(parent.get()).addChild(fragment);
				} else {
					rootFragment.addChild(fragment);
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
		// sometimes there are middle nodes, that are not assigned to a token in a fragment
		for (Map.Entry<ASTNode, ASTCodeFragment> unassignedEntry : nodesToFragments.entrySet()
				.stream()
				.filter(e -> e.getValue().getLevel() < 0)
				.sorted(Comparator.comparing(e -> e.getValue().getStart()))
				.collect(Collectors.toList())) {
			ASTCodeFragment fragment = unassignedEntry.getValue();
			ASTNode ancestorNode = unassignedEntry.getKey().getParent();
			if (ancestorNode == null) {
				throw new TestingException("Unable to calculate dependencies. Found unassignable node");
			}
			while (nodesToFragments.get(ancestorNode) == null) {
				ancestorNode = ancestorNode.getParent();
				if (ancestorNode == null) {
					throw new TestingException("Unable to calculate dependencies. Found unassignable node");
				}
			}
			ASTCodeFragment parentFragment = nodesToFragments.get(ancestorNode);
			parentFragment.addChild(fragment);
			fragment.setLevel(parentFragment.getLevel() + 1);
		}
		if (nodesToFragments.entrySet().stream().anyMatch(e -> e.getValue().getLevel() < 0)) {
			throw new TestingException("Unable to calculate dependencies. Cannot fix unassigned nodes");
		}
	}

	@Override
	protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
		Map<String, String> files = new HashMap<>();

		// add active fragments and all their children
		Map<String, Set<ICodeFragment>> fragmentsByFile = fragments.stream()
				.map(fr -> ((ASTCodeFragment) fr))
				.flatMap(fr -> m_isRecreating ? Stream.of(fr) : CollectionsUtility.getChildrenInDeep(fr).stream())
				.map(fr -> (ICodeFragment) fr)
				.collect(Collectors.groupingBy(ICodeFragment::getPath, Collectors.mapping(fr -> fr, Collectors.toSet())));

		// add fixed fragments without any children
		m_fixedFragments.forEach(fr -> {
			Set<ICodeFragment> f = fragmentsByFile.getOrDefault(fr.getPath(), new HashSet<>());
			f.add(fr);
			fragmentsByFile.put(fr.getPath(), f);
		});

		for (Map.Entry<String, Set<ICodeFragment>> entry : fragmentsByFile.entrySet()) {
			String fileName = entry.getKey();
			StringBuilder sb = new StringBuilder();
			entry.getValue().stream()
					.flatMap(fr -> ((ASTCodeFragment) fr).getTokens().stream())
					.sorted(Comparator.comparing(t -> t.start))
					.forEach(token -> sb.append(token.code));

			files.put(fileName, sb.toString());
		}
		return files;
	}

	public void addFixedFragments(List<ICodeFragment> minConfig) {
		m_fixedFragments.addAll(minConfig);
	}

	@Override
	public void recreateCode(List<ICodeFragment> fragments) {
		m_isRecreating = true;
		super.recreateCode(fragments);
	}
}
