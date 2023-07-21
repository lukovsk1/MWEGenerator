# MWE Generator

This generator produces minimal working examples (MWE) for Java code artefacts.
It features the following algorithms:

- DDmin (on code lines)
  - The DDmin algorithm is run on the set of all characters of the input problem.
  - This algorithm is very inefficient.
- DDmin (on single characters)
  - The DDmin algorithm is run on the set of code lines of the input problem.
- HDD
  - This algorithm is based on the Abstract-Syntax-Tree (AST) of the input problem.
  - Syntactical dependencies of the code are considered and allow for the usage of the more optimal Hierarchical DDmin (
    HDD) algorithm.
  - The preprocessing is more involved than with the other algorithms but the actual execution of the algorithm is more
    efficient.
- HDDr
  - The much more efficient recursive variant of the HDD algorithm
- GDD
  - This algorithm is based on the dependency graph of the input problem.
  - The preprocessing is even more involved than with HDD.
  - Uses less compiler calls.
- GDDr
  - The much more efficient recursive variant of GDD.

## Working with defects4j

It is possible to run the algorithm on problems extracted from [defects4j](https://github.com/rjust/defects4j).
Installing the `defects4j` CLI works best using a linux based system in WSL.
Using the `defects4j` CLI, a project with a reproducible bug can be downloaded to a local folder to run the MWE
Generator on it.

Example:

```
cd /mnt/c/dev/workspace/defects4j/

#  checking out bug 5 of commons-lang
defects4j checkout -p Lang -v 5b -w bugs/lang_5_b

cd bugs/lang_5_b

defects4j compile

defects4j test
```

Writing a corresponding run configuration will allow to run the MWEGenerator on this project.
The following parameters are necessary:

- The class name of the selected algorithm
  - see folder [generator](src/main/java/generator)
- The module path
  - The root folder of the project
- The source folder path
  - The relative path from the module to the java source code
- The unit test folder path
  - The relative path from the module to the unit tests
- The unit test method
  - The reference of the unit test method able to recreate the problem
- The expected result
  - The expected exception in the unit test

The following parameters are optional:

- Multiple runs (default false)
  - should the algorithm be instantly rerun on the output of the previous run?
- Timeout (default 4)
  - the timeout of the algorithm in hours

Example: defects4j lang_5

``` 
  generator.GDDMWEGenerator
  C:\dev\workspace\defects4j\bugs\lang_5_b
  src\main\java\
  src\test\java\
  org.apache.commons.lang3.LocaleUtilsTest#testLang865
  "java.lang.IllegalArgumentException: Invalid locale format: _GB"
  false
  4
```

The algorithm will first check if the defined unittest will indeed result in the expected exception.
If this does not work, you will see the message "Initial testing conditions are not met." in the console.

By enabling the options to log compilation and runtime errors, it is easy to see what went wrong. This can be changed
in [Main.java](src/main/java/Main.java).

If you run into a compilation error, the project might miss a dependency.
For now, they should be added to the `pom.xml` of the MWE Generator project.

If you run into a runtime error instead, this usually means, that the thrown exception does not match the one you
configured.
Changing the run configuration usually helps.
If there are any whitespaces in a line, it will be split up into mulitple lines, if the line isn't escaped.


## Compilation Type

It is possible to change between command line compilation and execution and the [InMemoryCompiler](https://github.com/trung/InMemoryJavaCompiler).
The measured execution times on a simple example were
- Command line: 151429ms
- In Memory: 7797ms

while delivering the same result. The InMemoryCompiler should be preferred and will be used as the default.

## Concurrent Execution

TestExecutorOptions#withNumberOfThreads allows the DDmin algorithm to be run in a configurable amount
of concurrent threads.
Multiple subsets of code fragments are compiled and executed in parallel.
For DDmin on the defects4j example CLI 1 this reduces the runtime for a single execution of the algorithm from around 6h
to 1.5h.

## Graph DB

The graph algorithms GDD and GDDr require a graph database to query the dependency graph.
We use [Neo4j](https://neo4j.com/) and its [driver](https://github.com/neo4j/neo4j-java-driver).
Version 4.4 is the last one that supports Java 8.

Running neo4j as a [Windows service](https://neo4j.com/docs/operations-manual/4.4/installation/windows/) seems to be
much more performant than using the docker image.
To disable authentication for the graph database, add the following line to `neo4j.conf`:

```
dbms.security.auth_enabled=false
```

Alternatively, if you want to use docker:

```
sudo docker run --publish=7474:7474 --publish=7687:7687 --env=NEO4J_AUTH=none neo4j:4.4
```

### Neo4j Snippets

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

## Running the algorithm on a Linux VM

Steps:

- Make sure java -version returns a java 8 version
- Install defects4j and check out a bug
- Clone the ddminj git repo
- `mvn clean compile assembly:single`
- `java -jar target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar ...args`
- Running GDD or GDDr requires an installation of neo4j
  - Make sure Java 11 is installed and JAVA_HOME points to its installation folder
  - Install version [4.4.19 of neo4j](https://neo4j.com/docs/operations-manual/4.4/installation/linux/)
  - Manually query the database with `cypher-shell`

