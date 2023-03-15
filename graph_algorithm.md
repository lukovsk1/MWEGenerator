# MWE Generator - Graph Algorithm

## Idea

The main idea of this algorithm

## Graph DB

As a graph database, we use [Neo4j](https://neo4j.com/) and its [driver](https://github.com/neo4j/neo4j-java-driver)  
Version 4.4 is the last one that supports Java 8.

```
sudo docker run --publish=7474:7474 --publish=7687:7687 --env=NEO4J_AUTH=none neo4j:4.4
```

## Snippets

Delete everything

```
match (a)-[r]->() delete a, r;
match (a) delete a;
```

Get whole graph for run "20230315_125558"

```
MATCH (n:Fragment_20230315_125558), (m:Token_20230315_125558) RETURN *
```