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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphTestExecutor extends ASTTestExecutor {
    private final GraphDB m_graphDB;

    public GraphTestExecutor(TestExecutorOptions options) {
        super(options);
        DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String nodeIdentifierSuffix = "_" + LocalDateTime.now().format(timeStampPattern);
        m_graphDB = new GraphDB(nodeIdentifierSuffix);
        System.out.println("Manually query the graph in the neo4j browser: http://localhost:7474/browser/");
        System.out.println("Example Query: \"MATCH (f:Fragment" + nodeIdentifierSuffix + ") RETURN *;\"");
    }

    @Override
    protected ASTCodeFragment transformToFragements(CompilationUnit javaAST, List<JavaParserUtility.Token> tokens, String relativeFileName, AtomicInteger fragmentNr) {
        ASTCodeFragment root = super.transformToFragements(javaAST, tokens, relativeFileName, fragmentNr);
        writeFragmentToDatabase(root, null);
        return null;
    }

    protected void writeFragmentToDatabase(ASTCodeFragment fragment, Node parentFragmentNode) {
        Node fragmentNode = m_graphDB.addFragmentNode(fragment);
        if (parentFragmentNode != null) {
            m_graphDB.addDependency(fragmentNode, parentFragmentNode);
        }
        fragment.getChildren().forEach(f -> writeFragmentToDatabase(f, fragmentNode));
    }

    @Override
    protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
        Set<Long> selectedActiveNodes = fragments.stream()
                .map(f -> (GraphCodeFragment) f)
                .map(ACodeFragment::getFragmentNumber)
                .collect(Collectors.toSet());
        return m_graphDB.mapFragmentsToFiles(selectedActiveNodes);
    }

    // returns the fragments that the ddmin algorithm should be run on at this moment
    public List<ICodeFragment> getActiveFragments() {
        return m_graphDB.calculateActiveFragments();
    }

    @Override
    public void addFixedFragments(List<ICodeFragment> fragments) {
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
                .collect(Collectors.toSet());
        m_graphDB.discardFragmentNodes(nodesToDiscard);
    }

    public int getNumberOfFragmentsInDB() {
        return m_graphDB.getNumberOfFragments();
    }
}
