package graph;

import fragment.ASTCodeFragment;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import utility.JavaParserUtility;

import java.util.Iterator;

public class GraphDB {

    public static final GraphDB INSTANCE = new GraphDB();
    private final static String NEO4J_URL = "bolt://localhost:7687";

    private final static String NODE_IDENTIFIER_FRAGMENT = "Fragment";
    private final static String NODE_IDENTIFIER_TOKEN = "Token";
    private final static String RELATIONSHIP_IDENTIFIER_HAS_TOKEN = "HAS_TOKEN";
    private final static String RELATIONSHIP_IDENTIFIER_DEPENDS_ON = "DEPENDS_ON";

    private static final String ATTR_FILENAME = "fileName";
    private static final String ATTR_START = "start";
    private static final String ATTR_END = "end";
    private static final String ATTR_CODE = "code";
    private final Driver driver;

    private GraphDB() {
        driver = GraphDatabase.driver(NEO4J_URL);
    }

    public static GraphDB getInstance() {
        return INSTANCE;
    }

    public Node addFragmentNode(String nodeIdentifierSuffix, ASTCodeFragment fragment) {
        if (fragment == null || fragment.getTokens().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Iterator<JavaParserUtility.Token> it = fragment.getTokens().iterator();
        JavaParserUtility.Token token = it.next();
        sb.append("CREATE (f:")
                .append(NODE_IDENTIFIER_FRAGMENT)
                .append(nodeIdentifierSuffix)
                .append("{")
                .append(ATTR_FILENAME)
                .append(":")
                .append(escape(fragment.getPath()))
                .append("})-[:")
                .append(RELATIONSHIP_IDENTIFIER_HAS_TOKEN)
                .append("]->(:Token")
                .append(nodeIdentifierSuffix)
                .append("{")
                .append(ATTR_START)
                .append(":")
                .append(token.start)
                .append(",")
                .append(ATTR_END)
                .append(":")
                .append(token.end)
                .append(",")
                .append(ATTR_CODE)
                .append(":")
                .append(escape(token.code))
                .append("})");
        while (it.hasNext()) {
            token = it.next();
            sb.append(", (f)-[:")
                    .append(RELATIONSHIP_IDENTIFIER_HAS_TOKEN)
                    .append("]->(:Token")
                    .append(nodeIdentifierSuffix)
                    .append("{")
                    .append(ATTR_START)
                    .append(":")
                    .append(token.start)
                    .append(",")
                    .append(ATTR_END)
                    .append(":")
                    .append(token.end)
                    .append(",")
                    .append(ATTR_CODE)
                    .append(":")
                    .append(escape(token.code))
                    .append("})");
        }
        sb.append(" RETURN f");
        Session session = driver.session();
        Result res = session.run(sb.toString());
        return res.single().get(0).asNode();
    }

    private String escape(String innerString) {
        return "'" + innerString + "'";
    }

    public Relationship addDependency(Node node, Node dependsOn) {
        StringBuilder sb = new StringBuilder();
        sb.append("MATCH (a), (b) WHERE ID(a)=")
                .append(node.id())
                .append(" AND ID(b)=")
                .append(dependsOn.id())
                .append(" CREATE (a)-[r:")
                .append(RELATIONSHIP_IDENTIFIER_DEPENDS_ON)
                .append("]->(b)")
                .append(" RETURN r");

        Session session = driver.session();
        Result res = session.run(sb.toString());
        return res.single().get(0).asRelationship();
    }
}
