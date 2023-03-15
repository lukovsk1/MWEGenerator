package graph;

import fragment.ASTCodeFragment;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_METHOD_NAME = "methodName";
	private static final String ATTR_CLASS_NAME = "className";
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
		ASTNode node = token.node;
		sb.append("CREATE (f:")
				.append(NODE_IDENTIFIER_FRAGMENT)
				.append(nodeIdentifierSuffix)
				.append("{")
				.append(ATTR_FILENAME)
				.append(":")
				.append(escape(fragment.getPath()))
				.append(",")
				.append(ATTR_CODE)
				.append(":")
				.append(escape(node.toString()))
				.append(",")
				.append(ATTR_TYPE)
				.append(":")
				.append(escape(node.getClass().getSimpleName()));

		addNodeSpecificAttribute(sb, node);

		sb.append("})-[:")
				.append(RELATIONSHIP_IDENTIFIER_HAS_TOKEN)
				.append("]->");
		writeTokenNode(sb, nodeIdentifierSuffix, token);
		while (it.hasNext()) {
			token = it.next();
			sb.append(", (f)-[:")
					.append(RELATIONSHIP_IDENTIFIER_HAS_TOKEN)
					.append("]->");
			writeTokenNode(sb, nodeIdentifierSuffix, token);
		}
		sb.append(" RETURN f");
		Session session = driver.session();
		Result res = session.run(sb.toString());
		return res.single().get(0).asNode();
	}

	private void addNodeSpecificAttribute(StringBuilder sb, ASTNode node) {
		if (node instanceof MethodDeclaration) {
			sb.append(",")
					.append(ATTR_METHOD_NAME)
					.append(":")
					.append(escape(((MethodDeclaration) node).getName().toString()));
		} else if (node instanceof ClassInstanceCreation) {
			sb.append(",")
					.append(ATTR_CLASS_NAME)
					.append(":")
					.append(escape(((ClassInstanceCreation) node).getType().toString()));
		} else if (node instanceof TypeDeclaration) {
			sb.append(",")
					.append(ATTR_CLASS_NAME)
					.append(":")
					.append(escape(((TypeDeclaration) node).getName().toString()));
		}
	}

	private void writeTokenNode(StringBuilder sb, String nodeIdentifierSuffix, JavaParserUtility.Token token) {
		sb.append("(:Token")
				.append(nodeIdentifierSuffix)
				.append(" {")
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
