# MWE Generator

Implementation of the DDmin algorithm to create a minimal working example (MWE) for Java and written in Java.

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

Example: defects4j lang_5

``` 
  C:\dev\workspace\defects4j\bugs\lang_5_b
  src\main\java\
  src\test\java\
  org.apache.commons.lang3.LocaleUtilsTest#testLang865
  "java.lang.IllegalArgumentException: Invalid locale format: _GB"
```

The algorithm will first check if the defined unittest will indeed result in the expected exception.
If this does not work, you will see the message "Initial testing conditions are not met." in the console.

By enabling the options to log compilation and runtime errors, it is easy to see what went wrong.

If you run into a compilation error, the project might miss a dependency.
For now, they should be added to the `pom.xml` of the MWE Generator project.

If you run into a runtime error instead, this usually means, that the thrown exception does not match the one you
configured.
Changing the run configuration usually helps.
If there are any whitespaces in a line, it will be split up into mulitple lines, if the line isn't escaped.

## Runtime and Optimization

Right now, the compilation of the code in each step of the ddmin algorithm dominates the runtime.

![](images/cpu_sample.png)

## TODOs

Next goals:

- [ ] Run the algorithm on a backward slicing of the unit-test run.
  - only consider relevant parts of the code
  - massively reduce the input of the algorithm
- [ ] Integrate static code analysis to the workflow
  - skip compilation if errors are present
  - save time compiling obviously wrong code
- [x] Algorithm for all DAGs (not just trees)
  - initial implementation based on AST
  - recognizing more dependencies in java code
- [ ] dynamically include dependencies of testes projects

## Compilation Type

It is possible to change between command line compilation and execution and the [InMemoryCompiler](https://github.com/trung/InMemoryJavaCompiler).
The measured execution times on a simple example were
- Command line: 151429ms
- In Memory: 7797ms

while delivering the same result. The InMemoryCompiler should be preferred and will be used as the default.

## Code Fragmentation 

There are currently three different code fragmentation methods implemented:

- Single Character
  - The DDmin algorithm is run on the set of all characters of the input problem.
  - This method is very inefficient.
  - Measured execution time: 475622ms
- Code Line
  - The DDmin algorithm is run on the set of code lines of the input problem.
  - It is possible to rerun the algorithm with the output of the previous run, which will improve the overall result.
  - Measured execution time:
    - Single run: 8197ms
    - With multiple runs: 11726ms
- AST
  - This algorithm is based on the Abstract-Syntax-Tree (AST) of the input problem.
  - Syntactical dependencies of the code are considered and allow for the usage of the more optimal Hierarchical DDmin (
    HDD) algorithm.
  - The preprocessing is more involved than with the other algorithms but the actual execution of the algorithm is more
    efficient.
  - Measured execution time: 6287ms

## Concurrent Execution

TestExecutorOptions#withConcurrentExecution allows the DDmin algorithm to be run in currently in a configurable amount
of concurrent threads.
Multiple subsets of code fragments are compiled and executed in parallel.
For the defects4j example CLI 1 this reduces the runtime for a single execution of the algorithm from around 6h to 1.5h.

## Running the algorithm on a Linux VM

Steps:

- Install defects4j and check out a bug
- Clone the ddminj git repo
- mvn clean compile assembly:single
- java -jar target/ddminj-1.0-SNAPSHOT-jar-with-dependencies.jar ...

