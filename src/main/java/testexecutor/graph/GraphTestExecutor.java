package testexecutor.graph;

import fragment.ACodeFragment;
import fragment.ASTCodeFragment;
import fragment.GraphCodeFragment;
import fragment.ICodeFragment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.neo4j.driver.types.Node;
import testexecutor.TestExecutorOptions;
import testexecutor.ast.ASTTestExecutor;
import utility.JavaParserUtility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphTestExecutor extends ASTTestExecutor {
    private final GraphDB m_graphDB;
    private final Map<Long, GraphCodeFragment> m_fragments;
    private final Set<ICodeFragment> m_fixedFragments;

    public GraphTestExecutor(TestExecutorOptions options) {
        super(options);
        DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String nodeIdentifierSuffix = "_" + LocalDateTime.now().format(timeStampPattern);
        m_graphDB = new GraphDB(nodeIdentifierSuffix);
        System.out.println("Manually query the graph in the neo4j browser: http://localhost:7474/browser/");
        System.out.println("Example Query: \"MATCH (f:Fragment" + nodeIdentifierSuffix + ") RETURN *;\"");

        m_fragments = new HashMap<>();
        m_fixedFragments = new HashSet<>();
    }

    @Override
    protected ASTCodeFragment transformToFragements(CompilationUnit javaAST, List<JavaParserUtility.Token> tokens, String relativeFileName, AtomicInteger fragmentNr) {
        ASTCodeFragment root = super.transformToFragements(javaAST, tokens, relativeFileName, fragmentNr);
        writeFragmentToDatabase(root, null);
        return null;
    }

    protected void writeFragmentToDatabase(ASTCodeFragment fragment, Node parentFragmentNode) {
        Node fragmentNode = m_graphDB.addFragmentNode(fragment);
        m_fragments.put(fragmentNode.id(), new GraphCodeFragment(fragment.getPath(), fragmentNode.id(), fragment.getTokens()));
        if (parentFragmentNode != null) {
            m_graphDB.addDependency(fragmentNode, parentFragmentNode);
        }
        fragment.getChildren().forEach(f -> writeFragmentToDatabase(f, fragmentNode));
    }

    @Override
    protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> activeFragments) {
        List<ICodeFragment> fragments = new ArrayList<>(activeFragments);
        fragments.addAll(m_fixedFragments);
        Set<Long> selectedActiveNodes = activeFragments.stream()
                .map(GraphCodeFragment.class::cast)
                .map(ACodeFragment::getFragmentNumber)
                .collect(Collectors.toSet());
        fragments.addAll((m_graphDB.getIdsOfDependentNodes(selectedActiveNodes)
                .stream()
                .map(m_fragments::get)
                .filter(Objects::nonNull).collect(Collectors.toList())));

        Map<String, Set<GraphCodeFragment>> fragmentsByFile = fragments
                .stream()
                .map(GraphCodeFragment.class::cast)
                .collect(Collectors.groupingBy(ICodeFragment::getPath, Collectors.mapping(fr -> fr, Collectors.toSet())));

        Map<String, String> files = new HashMap<>();
        for (Map.Entry<String, Set<GraphCodeFragment>> entry : fragmentsByFile.entrySet()) {
            String fileName = entry.getKey();
            StringBuilder sb = new StringBuilder();
            entry.getValue().stream()
                    .flatMap(fr -> fr.getTokens().stream())
                    .sorted(Comparator.comparing(t -> t.start))
                    .forEach(token -> sb.append(token.code));

            files.put(fileName, sb.toString());
        }

        return files;
    }

    // returns the fragments that the ddmin algorithm should be run on at this moment
    public List<ICodeFragment> getActiveFragments() {
        return m_graphDB.calculateActiveFragments().stream()
                .map(m_fragments::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void addFixedFragments(List<ICodeFragment> fragments) {
        m_fixedFragments.addAll(fragments);
        Set<Long> fixedNodes = fragments.stream()
                .map(f -> (GraphCodeFragment) f)
                .map(ACodeFragment::getFragmentNumber)
                .collect(Collectors.toSet());
        m_graphDB.markFragmentNodesAsFixed(fixedNodes);
    }

    public void addDiscardedFragments(List<ICodeFragment> fragments) {
        Set<Long> nodesToDiscard = fragments.stream()
                .map(f -> (GraphCodeFragment) f)
                .map(ACodeFragment::getFragmentNumber)
                .peek(m_fragments::remove)
                .collect(Collectors.toSet());
        m_graphDB.discardFragmentNodes(nodesToDiscard);
    }

    public int getNumberOfFragmentsInDB() {
        return m_graphDB.getNumberOfFragments();
    }
}
