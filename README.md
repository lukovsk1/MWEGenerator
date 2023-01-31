# DDminJ

Implementation of the DDmin algorithm to create a minimal working example (MWE) for Java and written in Java.

## TODOs

- [x] set up error example and unit-test
- [x] basic implementation of DDmin algorithm
- [x] code fragment extraction
- [x] test executor
- [ ] automatic code refactoring
- [ ] input data reduction

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
  - Syntactical dependencies of the code  are considered and allow for the usage of the more optimal Hierarchical DDmin (HDD) algorithm.
  - The preprocessing is more involved than with the other algorithms but the actual execution of the algorithm is more efficient.
  - Measured execution time: 6287ms