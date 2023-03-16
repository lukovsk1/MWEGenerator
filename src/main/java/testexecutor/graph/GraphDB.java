package testexecutor.graph;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

	private static final AtomicInteger parameterNumber = new AtomicInteger();
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
		Map<String, Object> params = new HashMap<>();
		Iterator<JavaParserUtility.Token> it = fragment.getTokens().iterator();
		JavaParserUtility.Token token = it.next();
		ASTNode node = token.node;
		sb.append("CREATE (f:")
				.append(NODE_IDENTIFIER_FRAGMENT)
				.append(nodeIdentifierSuffix)
				.append("{");
		addAttribute(sb, params, ATTR_FILENAME, fragment.getPath());
		addAttribute(sb, params, ATTR_CODE, node.toString());
		addAttribute(sb, params, ATTR_TYPE, node.getClass().getSimpleName());

		addNodeSpecificAttribute(sb, params, node);

		sb.append("})-[:")
				.append(RELATIONSHIP_IDENTIFIER_HAS_TOKEN)
				.append("]->");
		writeTokenNode(sb, params, nodeIdentifierSuffix, token);
		while (it.hasNext()) {
			token = it.next();
			sb.append(", (f)-[:")
					.append(RELATIONSHIP_IDENTIFIER_HAS_TOKEN)
					.append("]->");
			writeTokenNode(sb, params, nodeIdentifierSuffix, token);
		}
		sb.append(" RETURN f");
		Session session = driver.session();
		Result res = session.run(sb.toString(), params);
		return res.single().get(0).asNode();
	}

	private void addNodeSpecificAttribute(StringBuilder sb, Map<String, Object> parameters, ASTNode node) {
		if (node instanceof MethodDeclaration) {
			addAttribute(sb, parameters, ATTR_METHOD_NAME, ((MethodDeclaration) node).getName().toString());
		} else if (node instanceof ClassInstanceCreation) {
			addAttribute(sb, parameters, ATTR_CLASS_NAME, ((ClassInstanceCreation) node).getType().toString());
		} else if (node instanceof TypeDeclaration) {
			addAttribute(sb, parameters, ATTR_CLASS_NAME, ((TypeDeclaration) node).getName().toString());
		}
	}

	private void writeTokenNode(StringBuilder sb, Map<String, Object> params, String nodeIdentifierSuffix, JavaParserUtility.Token token) {
		sb.append("(:")
				.append(NODE_IDENTIFIER_TOKEN)
				.append(nodeIdentifierSuffix)
				.append(" {");
		addAttribute(sb, params, ATTR_START, token.start);
		addAttribute(sb, params, ATTR_END, token.end);
		addAttribute(sb, params, ATTR_CODE, token.code)
				.append("})");
	}

	private StringBuilder addAttribute(StringBuilder sb, Map<String, Object> parameters, String attributeName, Object value) {
		char lastChar = sb.charAt(sb.length() - 1);
		if (lastChar != '{' && lastChar != ',') {
			sb.append(",");
		}
		int paramNumber = parameterNumber.getAndIncrement();
		sb.append(attributeName)
				.append(":$")
				.append(paramNumber);
		parameters.put(String.valueOf(paramNumber), value);
		return sb;
	}

	public Relationship addDependency(Node node, Node dependsOn) {
		String query = "MATCH (a), (b) WHERE ID(a)=" +
				node.id() +
				" AND ID(b)=" +
				dependsOn.id() +
				" CREATE (a)-[r:" +
				RELATIONSHIP_IDENTIFIER_DEPENDS_ON +
				"]->(b)" +
				" RETURN r";

		Session session = driver.session();
		Result res = session.run(query);
		return res.single().get(0).asRelationship();
	}
}
