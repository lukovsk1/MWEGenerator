# MWE Generator - Graph Algorithm

## Idea

The main idea of this algorithm is to run a generalized version of the
HDD algorithm on any directed acyclic graph representing the code and its internal dependencies.

## Pseudocode algorithm

- G = (V,E)
  - the graph
- V
  - the set of code fragments
- E
  - the set of dependencies between the fragments.
  - i.e. (v,w) in E means that fragment v depends on fragment w

```
free := V
fixed := {}
discarded := {}

while free != {}
    active := {f in free | s.t. not exists (f,v) in E with v in free}
    free := free \ active
    minconfig := ddmin(active)
    fixed := fixed u minconfig
    discarded := discarded u (active \ minconfig)

return fixed
```

The code that the ddmin algorithm tests on will always contain all the fixed fragments, never contain any discarded
fragments and will contain all fragments in free that not dependent on any fragment missing in the current
configuration (a subset of active).

The set of fixed fragments that is returned at the end of the run, is an MWE.

## Graph DB

As a graph database, we use [Neo4j](https://neo4j.com/) and its [driver](https://github.com/neo4j/neo4j-java-driver)  
Version 4.4 is the last one that supports Java 8.

```
sudo docker run --publish=7474:7474 --publish=7687:7687 --env=NEO4J_AUTH=none neo4j:4.4
```

## Snippets

Delete everything

```
match (a) detach delete a;
```

Get whole graph for run "20230315_125558"

```
MATCH (n:Fragment_20230315_125558), (m:Token_20230315_125558) RETURN *;
```

Find root nodes

```
match (f:Fragment_20230315_133249) where not exists((f)-[:DEPENDS_ON]->(:Fragment_20230315_133249)) return f;
```