package testexecutor.graph;

import fragment.ASTCodeFragment;
import fragment.ICodeFragment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.neo4j.driver.types.Node;
import testexecutor.ExecutorConstants;
import testexecutor.TestExecutorOptions;
import testexecutor.ast.ASTTestExecutor;
import utility.JavaParserUtility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphTestExecutor extends ASTTestExecutor {
    private final GraphDB m_graphDB;
    private final String m_nodeIdentifierSuffix;

    public GraphTestExecutor(TestExecutorOptions options) {
        super(options);
        m_graphDB = GraphDB.getInstance();
        DateTimeFormatter timeStampPattern = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        m_nodeIdentifierSuffix = "_" + LocalDateTime.now().format(timeStampPattern);
        System.out.println("Intialized GraphExtractor with node identifier suffix " + m_nodeIdentifierSuffix);
    }

    public static void main(String[] args) {
        GraphTestExecutor extractor = new GraphTestExecutor(ExecutorConstants.CALCULATOR_OPTIONS_MULTI);
        extractor.initialize();
        extractor.extractFragments();
    }

    @Override
    protected ASTCodeFragment transformToFragements(CompilationUnit javaAST, List<JavaParserUtility.Token> tokens, String relativeFileName, AtomicInteger fragmentNr) {
        ASTCodeFragment root = super.transformToFragements(javaAST, tokens, relativeFileName, fragmentNr);
        writeFragmentToDatabase(root, null);
        return null;
    }

    protected void writeFragmentToDatabase(ASTCodeFragment fragment, Node parentFragmentNode) {
        Node fragmentNode = m_graphDB.addFragmentNode(m_nodeIdentifierSuffix, fragment);
        if (parentFragmentNode != null) {
            m_graphDB.addDependency(fragmentNode, parentFragmentNode);
        }
        fragment.getChildren().forEach(f -> writeFragmentToDatabase(f, fragmentNode));
    }

    @Override
    protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
        // ignore input. reading fragments from graph db

        //TODO impl
        return null;
    }
}
