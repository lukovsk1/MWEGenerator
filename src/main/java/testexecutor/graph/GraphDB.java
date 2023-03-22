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
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import utility.JavaParserUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphDB {

	private final static String NEO4J_URL = "bolt://localhost:7687";
	private final static String LABEL_PREFIX_FRAGMENT = ":Fragment";
	private static final String LABEL_FIXED = ":Fixed";
	private static final String LABEL_FREE = ":Free";
	private static final String LABEL_ACTIVE = ":Active";
	private final static String RELATIONSHIP_LABEL_DEPENDS_ON = ":DEPENDS_ON";

	private static final String ATTR_FILENAME = "fileName";
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
		JavaParserUtility.Token token = fragment.getTokens().get(0);
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

		sb.append("}) RETURN f");
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

	private void addAttribute(StringBuilder sb, Map<String, Object> parameters, String attributeName, Object value) {
		char lastChar = sb.charAt(sb.length() - 1);
		if (lastChar != '{' && lastChar != ',') {
			sb.append(",");
		}
		int paramNumber = parameterNumber.getAndIncrement();
		sb.append(attributeName)
				.append(":$")
				.append(paramNumber);
		parameters.put(String.valueOf(paramNumber), value);
	}

	public void addDependency(Node node, Node dependsOn) {
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
		res.single().get(0).asRelationship();
	}

	public void markFragmentNodesAsFixed(Set<Long> nodeIds) {
		if (nodeIds.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<>();

		String query = "MATCH (n" +
				LABEL_PREFIX_FRAGMENT +
				m_nodeIdentifierSuffix +
				")" +
				" WHERE ID(n) IN $nodeIds" +
				" SET n " +
				LABEL_FIXED +
				" REMOVE n" +
				LABEL_ACTIVE +
				";";
		params.put("nodeIds", nodeIds);

		Session session = m_driver.session();
		session.run(query, params);
	}

	public void discardFragmentNodes(Set<Long> nodeIds) {
		if (nodeIds.isEmpty()) {
			return;
		}
		Map<String, Object> params = new HashMap<>();
		params.put("nodeIds", nodeIds);
		Session session = m_driver.session();

		// delete all dependent fragments
		String query1 = "MATCH (f" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ")<-[" + RELATIONSHIP_LABEL_DEPENDS_ON + "*]-(u) WHERE ID(f) IN $nodeIds DETACH DELETE u";
		session.run(query1, params).consume();

		// delete the fragments to discard
		String query2 = "MATCH (f" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ") WHERE ID(f) IN $nodeIds DETACH DELETE f";
		session.run(query2, params).consume();
	}

	public int getNumberOfFragments() {
		String query = "MATCH (n" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + ") RETURN COUNT(n);";
		Session session = m_driver.session();
		Result res = session.run(query);
		return res.single().get(0).asInt();
	}

	public Set<Long> calculateActiveFragments() {
		String query = "MATCH (n" + LABEL_PREFIX_FRAGMENT + m_nodeIdentifierSuffix + LABEL_FREE
				+ ") WHERE NOT EXISTS {MATCH (n)-[" + RELATIONSHIP_LABEL_DEPENDS_ON + "]->(" + LABEL_FREE
				+ ")} SET n" + LABEL_ACTIVE + " REMOVE n" + LABEL_FREE + " RETURN n;";

		// TODO: check if a limit makes sense. The ddmin algorithm might perform much better on bounded sets.

		Session session = m_driver.session();
		Result res = session.run(query);
		return res.stream()
				.map(rec -> rec.get(0).asNode())
				.filter(Objects::nonNull)
				.map(Entity::id)
				.collect(Collectors.toSet());
	}

	public Set<Long> getIdsOfDependentNodes(Set<Long> selectedActiveNodes) {
		/*
			MATCH (a:Fragment_20230316_231802:Active), (a)<-[:DEPENDS_ON*]-(f:Fragment_20230316_231802:Free)
			WHERE ID(a) IN [123]
			RETURN ID(f)
			UNION
			MATCH (f:Fragment_20230316_231802:Fixed)
			RETURN ID(f)
		 */
		Map<String, Object> params = new HashMap<>();
		String query = "MATCH (a" +
				LABEL_PREFIX_FRAGMENT +
				m_nodeIdentifierSuffix +
				LABEL_ACTIVE +
				"), (a)<-[" +
				RELATIONSHIP_LABEL_DEPENDS_ON +
				"*]-(f" +
				LABEL_PREFIX_FRAGMENT +
				m_nodeIdentifierSuffix +
				LABEL_FREE +
				") WHERE ID(a) IN $nodeIds" +
				" RETURN ID(f)";

		params.put("nodeIds", selectedActiveNodes);

		Session session = m_driver.session();
		Result res = session.run(query, params);
		return res.stream()
				.map(rec -> rec.get(0).asLong())
				.collect(Collectors.toSet());
	}
}
