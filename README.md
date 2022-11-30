# Flexeme Dataflow Extraction

`FlexemePdgGenerator` runs the analysis and extracts a Flexeme-compatible PDG graph.

Run with `java -cp codechanges-checker-all.jar org.checkerframework.checker.codechanges.FlexemePdgGenerator <filepath>`

Requires Java 11. Otherwise you'll get an Illegal Access Error with Java 17.

## More information

The Flexeme Dataflow Extractor is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
