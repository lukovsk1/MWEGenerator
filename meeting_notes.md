## Meeting Notes

### 15.03.2023

- The MWE-Generator with the HDD-algorithm performs well on small examples but does not scale to larger projects right
  now.
- Instead of measuring runtime, it would be more interesting to count compiler calls and the ratio of successful
  compilations to failed compilations. The metrics are independent of hardware.
- Add even more logging
- There are seemingly no slicers for java that generate a working artifact out-of-the-box.
- Actually generating a minimal working example is not part of slicing algorithms and coverage tools.
- For the moment, slicing will not be pursued further.
- The basic ddmin algorithm (line- or even character-based) can be used as a baseline to measure the performance of
  further algorithmic improvements.
- Holding the model in a graph-db while running the algorithm brings multiple advantages:
    - Ability to query model at will
    - Graphical representation of data in graph database tool
    - Performant queries on structured data
- Multiple runs of the algorithm should yield a better result. But later runs might not be able to further optimize the
  result significantly.
- Write intermediate results (not yet minimal working examples) to disk more regularly.
- Measure progress (after each level, after each run)
- The starting point for the graph algorithm is the HDD algorithm. We can add additional dependency between code
  fragments to the graph.
- First new dependency: Usage of Objects depends on their import, imports depend on the existence of the imported
  class (if not an external dependency).


- Next steps:
    - Finish implementing graph algorithm with AST dependencies (should perform exactly like HDD)
    - Add additional logging
    - Add import dependencies