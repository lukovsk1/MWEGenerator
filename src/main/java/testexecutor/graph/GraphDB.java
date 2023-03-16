package testexecutor.graph;

import fragment.ASTCodeFragment;
import fragment.GraphCodeFragment;
import fragment.ICodeFragment;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import utility.JavaParserUtility;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphDB {

	private final static String NEO4J_URL = "bolt://localhost:7687";
	private final static String LABEL_PREFIX_FRAGMENT = ":Fragment";
	private final static String LABEL_PREFIX_TOKEN = ":Token";
	private static final String LABEL_FIXED = ":Fixed";
	private static final String LABEL_FREE = ":Free";
	private static final String LABEL_ACTIVE = ":Active";
	private final static String RELATIONSHIP_LABEL_HAS_TOKEN = ":HAS_TOKEN";
	private final static String RELATIONSHIP_LABEL_DEPENDS_ON = ":DEPENDS_ON";

	private static final String ATTR_FILENAME = "fileName";
	private static final String ATTR_START = "start";
	private static final String ATTR_END = "end";
	private static final String ATTR_CODE = "code";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_METHOD_NAME = "methodName";
	private static final String ATTR_CLASS_NAME = "className";

	private static final AtomicInteger parameterNumber = new AtomicInteger();
	private final Driver m_driver;
	private final String m_nodeIdentifierSuffix;

	public GraphDB(String nodeIdentifierSuffix) {
		m_driver = GraphDatabase.driver(NEO4J_URL);
		m_nodeIdentifierSuffix = nodeIdentifierSuffix;
	}

	public Node addFragmentNode(ASTCodeFragment fragment) {
		if (fragment == null || fragment.getTokens().isEmpty()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		Map<String, Object> params = new HashMap<>();
		Iterator<JavaParserUtility.Token> it = fragment.getTokens().iterator();
		JavaParserUtility.Token token = it.next();
		ASTNode node = token.node;
		sb.append("CREATE (f")
				.append(LABEL_PREFIX_FRAGMENT)
				.append(m_nodeIdentifierSuffix)
				.append(LABEL_FREE)
				.append(" {");
		addAttribute(sb, params, ATTR_FILENAME, fragment.getPath());
		addAttribute(sb, params, ATTR_CODE, node.toString());
		addAttribute(sb, params, ATTR_TYPE, node.getClass().getSimpleName());

		addNodeSpecificAttribute(sb, params, node);

		sb.append("})-[")
				.append(RELATIONSHIP_LABEL_HAS_TOKEN)
				.append("]->");
		writeTokenNode(sb, params, m_nodeIdentifierSuffix, token);
		while (it.hasNext()) {
			token = it.next();
			sb.append(", (f)-[")
					.append(RELATIONSHIP_LABEL_HAS_TOKEN)
					.append("]->");
			writeTokenNode(sb, params, m_nodeIdentifierSuffix, token);
		}
		sb.append(" RETURN f");
		Session session = m_driver.session();
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
		sb.append("(")
				.append(LABEL_PREFIX_TOKEN)
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
				" CREATE (a)-[r" +
				RELATIONSHIP_LABEL_DEPENDS_ON +
				"]->(b)" +
				" RETURN r";

		Session session = m_driver.session();
		Result res = session.run(query);
		return res.single().get(0).asRelationship();
	}

	public void markFragmentNodesAsFixed(Set<Long> nodeIds) {
		if (nodeIds.isEmpty()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		Map<String, Object> params = new HashMap<>();

		sb.append("MATCH (n")
				.append(LABEL_PREFIX_FRAGMENT)
				.append(m_nodeIdentifierSuffix)
				.append(")")
				.append(" WHERE ID(n) IN $nodeIds")
				.append(" SET n ")
				.append(LABEL_FIXED)
				.append(" REMOVE n")
				.append(LABEL_ACTIVE)
				.append(";");
		params.put("nodeIds", nodeIds);

		Session session = m_driver.session();
		session.run(sb.toString(), params);
	}

	public void discardFragmentNodes(Set<Long> nodeIds) {
		if (nodeIds.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<>();
		params.put("nodeIds", nodeIds);
		Session session = m_driver.session();

		// delete the tokens of all dependent fragments
		String query1 = "MATCH (f" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ")<-[" + RELATIONSHIP_LABEL_DEPENDS_ON + "*]-(u)-["
				+ RELATIONSHIP_LABEL_HAS_TOKEN + "]->(t) WHERE ID(f) IN $nodeIds DETACH DELETE t";
		session.run(query1, params).consume();

		// delete all dependent fragments
		String query2 = "MATCH (f" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ")<-[" + RELATIONSHIP_LABEL_DEPENDS_ON + "*]-(u) WHERE ID(f) IN $nodeIds DETACH DELETE u";
		session.run(query2, params).consume();

		// delete the tokens of the fragments to discard
		String query3 = "MATCH (f" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ")-[" + RELATIONSHIP_LABEL_HAS_TOKEN + "]->(t) WHERE ID(f) IN $nodeIds DETACH DELETE t";
		session.run(query3, params).consume();

		// delete the fragments to discard
		String query4 = "MATCH (f" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ") WHERE ID(f) IN $nodeIds DETACH DELETE f";
		session.run(query4, params).consume();
	}

	public int getNumberOfFragments() {
		String query = "MATCH (n" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ") RETURN COUNT(n);";
		Session session = m_driver.session();
		Result res = session.run(query);
		return res.single().get(0).asInt();
	}

	public List<ICodeFragment> calculateActiveFragments() {
		String query = "MATCH (n" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + LABEL_FREE
				+ ") WHERE NOT EXISTS {MATCH (n)-[" + RELATIONSHIP_LABEL_DEPENDS_ON + "]->(" + LABEL_FREE
				+ ")} SET n" + LABEL_ACTIVE + " REMOVE n" + LABEL_FREE + " RETURN n;";

		// TODO: check if a limit makes sense. The ddmin algorithm might perform much better on bounded sets.

		Session session = m_driver.session();
		Result res = session.run(query);
		return res.stream()
				.map(rec -> rec.get(0).asNode())
				.filter(Objects::nonNull)
				.map(node -> new GraphCodeFragment(node.get(ATTR_FILENAME).asString(), node.id()))
				.collect(Collectors.toList());
	}

	public Map<String, String> mapFragmentsToFiles(Set<Long> selectedActiveNodes) {
		/*
			CALL {
				MATCH (f:Fragment_20230316_231802:Active)
				WHERE ID(f) IN [123]
				RETURN f
				UNION
				MATCH (a:Fragment_20230316_231802:Active), (a)<-[:DEPENDS_ON*]-(f:Fragment_20230316_231802:Free)
				WHERE ID(a) IN [123]
				RETURN f
				UNION
				MATCH (f:Fragment_20230316_231802:Fixed)
				RETURN f
			}
			WITH f
			MATCH (f)-[:HAS_TOKEN]->(t:Token_20230316_231802)
			WITH f.fileName as fileName, t ORDER BY t.start
			WITH fileName, COLLECT(t.code) as c
			WITH fileName, REDUCE(s=HEAD(c), n IN TAIL(c) | s + n) AS code
			RETURN fileName, code;
		 */
		StringBuilder sb = new StringBuilder();
		Map<String, Object> params = new HashMap<>();
		sb.append("CALL { ")
				.append("MATCH (f")
				.append(LABEL_PREFIX_FRAGMENT)
				.append(m_nodeIdentifierSuffix)
				.append(") WHERE ID(f) IN $nodeIds")
				.append(" RETURN f")
				.append(" UNION ")
				.append("MATCH (a")
				.append(LABEL_PREFIX_FRAGMENT)
				.append(m_nodeIdentifierSuffix)
				.append(LABEL_ACTIVE)
				.append("), (a)<-[")
				.append(RELATIONSHIP_LABEL_DEPENDS_ON)
				.append("*]-(f")
				.append(LABEL_PREFIX_FRAGMENT)
				.append(m_nodeIdentifierSuffix)
				.append(LABEL_FREE)
				.append(") WHERE ID(a) IN $nodeIds")
				.append(" RETURN f")
				.append(" UNION ")
				.append("MATCH (f")
				.append(LABEL_PREFIX_FRAGMENT)
				.append(m_nodeIdentifierSuffix)
				.append(LABEL_FIXED)
				.append(") RETURN f")
				.append(" } WITH f ")
				.append("MATCH (f)-[")
				.append(RELATIONSHIP_LABEL_HAS_TOKEN)
				.append("]->(t")
				.append(LABEL_PREFIX_TOKEN)
				.append(m_nodeIdentifierSuffix)
				.append(") WITH f.")
				.append(ATTR_FILENAME)
				.append(" as fileName, t ORDER BY t.")
				.append(ATTR_START)
				.append(" WITH fileName, COLLECT(t.")
				.append(ATTR_CODE)
				.append(") as c ")
				.append(" WITH fileName, REDUCE(s=HEAD(c), n IN TAIL(c) | s + n) AS code ")
				.append("RETURN fileName, code;");

		params.put("nodeIds", selectedActiveNodes);

		Session session = m_driver.session();
		Result res = session.run(sb.toString(), params);
		Map<String, String> filesToCode = new HashMap<>();
		while (res.hasNext()) {
			Record rec = res.next();
			filesToCode.put(rec.get(0).asString(), rec.get(1).asString());
		}
		return filesToCode;
	}
}
