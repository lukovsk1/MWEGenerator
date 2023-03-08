package graph;

import org.neo4j.driver.*;

import java.util.Map;

public class GraphDB {

    public static final GraphDB INSTANCE = new GraphDB();
    private final static String NEO4J_URL = "bolt://localhost:7687";
    private final Driver driver;

    private GraphDB() {
        driver = GraphDatabase.driver(NEO4J_URL);
    }

    public static GraphDB getInstance() {
        return INSTANCE;
    }

    public Record addNode(String nodeIdentifier, Map<String, Object> attributes) {
        Session session = driver.session();
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE (n:")
                .append(nodeIdentifier)
                .append(") ");
        for (String attribute : attributes.keySet()) {
            sb.append("SET n.").append(attribute).append(" = $").append(attribute).append(" ");
        }
        sb.append(" RETURN n");
        Result res = session.run(sb.toString(), attributes);
        return res.single();
    }
}
