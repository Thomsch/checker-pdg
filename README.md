# Flexeme PDG Extractor

`PdgExtractor` analyze a Java file and extracts a Flexeme-compatible PDG with dataflow and nameflow.

### Usage
1. Run `./gradlew shadowJar` to build the jar file.
2. Run the analysis with `java -cp path/to/jar org.checkerframework.flexeme.PdgExtractor <file/to/analyze> <sourcepath> <classpath>`

### Java 17
To avoid Illegal Access Error from Java 17, you need to add the compiler options described in the [Checker Framework manual](https://checkerframework.org/manual/#javac-jdk11). 
There is an example available in build file of the [Div By Zero Checker](https://github.com/kelloggm/div-by-zero-checker/blob/master/build.gradle).

## More information
The Flexeme PDG Extractor is built upon the Checker Framework.  Please see
the [Checker Framework Manual](https://checkerframework.org/manual/) for
more information about using pluggable type-checkers, including this one.
