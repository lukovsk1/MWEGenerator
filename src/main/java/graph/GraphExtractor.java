package graph;

import fragment.ASTCodeFragment;
import fragment.ICodeFragment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.neo4j.driver.Record;
import testexecutor.TestExecutorOptions;
import testexecutor.ast.ASTTestExecutor;
import utility.JavaParserUtility;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GraphExtractor extends ASTTestExecutor {
    private final GraphDB m_graphDB;
    private final String m_nodeIdentifier;

    public GraphExtractor(TestExecutorOptions options) {
        super(options);
        m_graphDB = GraphDB.getInstance();
        m_nodeIdentifier = "GraphExtractor_" + LocalDateTime.now();
    }

    @Override
    protected ASTCodeFragment transformToFragements(CompilationUnit javaAST, List<JavaParserUtility.Token> tokens, String relativeFileName, AtomicInteger fragmentNr) {
        ASTCodeFragment root = super.transformToFragements(javaAST, tokens, relativeFileName, fragmentNr);
        writeFragmentToDatabase(root);
        return null;
    }

    protected void writeFragmentToDatabase(ASTCodeFragment fragment) {
        Set<Record> records = new HashSet<>();
        for (JavaParserUtility.Token token : fragment.getTokens()) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("fileName", fragment.getPath());
            attributes.put("start", token.start);
            attributes.put("end", token.end);
            attributes.put("code", token.code);
            records.add(m_graphDB.addNode(m_nodeIdentifier, attributes));
        }

        fragment.getChildren().forEach(this::writeFragmentToDatabase);
    }

    @Override
    protected Map<String, String> mapFragmentsToFiles(List<ICodeFragment> fragments) {
        // ignore input. reading fragments from graph db

        //TODO impl
        return null;
    }
}
