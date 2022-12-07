# Flexeme Dataflow Extraction

`FlexemePdgGenerator` runs the analysis and extracts a Flexeme-compatible PDG graph.

Run with `java -cp codechanges-checker-all.jar org.checkerframework.checker.codechanges.FlexemePdgGenerator <filepath>`

To avoid Illegal Access Error from Java 17, you need to add the compiler options described in the [Checker Framework manual](https://checkerframework.org/manual/#javac-jdk11). 
There is an example available in build file of the [Div By Zero Checker](https://github.com/kelloggm/div-by-zero-checker/blob/master/build.gradle).

## More information

The Flexeme Dataflow Extractor is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
