# DDminJ

Implementation of the DDmin algorithm to create a minimal working example (MWE) for Java and written in Java.

## TODOs

- [x] set up error example and unit-test
- [x] basic implementation of DDmin algorithm
- [x] code slice extraction
- [x] test executor
- [ ] automatic code refactoring
- [ ] code slicing
- [ ] input data reduction

## Compilation Type

It is possible to change between command line compilation and execution and the [InMemoryCompiler](https://github.com/trung/InMemoryJavaCompiler).
The measured execution times on a simple example were
- Command line: 151429ms
- In Memory: 7797ms

while delivering the same result. The InMemoryCompiler should be preferred and will be used as the default.