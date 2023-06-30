package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

/**
 * General tests for the PDG extraction.
 */
public class PdgMethodExtractionTest {

    // TODO: Check number of PDG elements is right and that the PDG elements are right.
    @Test
    public void testSourcePath() throws Throwable {

        String inputFile = "src/test/resources/org/a/A.java";
        String sourcepath = "src/test/resources";

        PdgExtractor.compileFile(inputFile, "build/", false, sourcepath, "");
    }

    @Test
    public void testClassPath() throws Throwable {

        String inputFile = "src/test/resources/org/C.java";
        String classpath = "src/test/resources/guava-31.1-jre.jar";

        PdgExtractor.compileFile(inputFile, "build/", false, "", classpath);
    }

    @Test
    public void testDataFlow() {
        String inputFile1 = "src/test/resources/BasicTests.java";
        PdgExtractor.compileFile(inputFile1, "build/", false, "", "");

        String inputFile2 = "src/test/resources/DataFlowTests.java";
        PdgExtractor.compileFile(inputFile2, "build/", false, "", "");
    }

    @Test
    public void testEnum() {
        String inputFile1 = "src/test/resources/MyEnum.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile1, "", "", "build/Enum.dot");
    }

    @Test
    public void testAbstract() {
        String inputFile = "src/test/resources/MyAbstract.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "", "", "build/Abstract.dot");
    }

    @Test
    public void testInfiniteLoop() {
        String inputFile = "src/test/resources/Infinite.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Infinite.dot");
    }

    @Test
    public void testAnonymous() {
        String inputFile = "src/test/resources/BasicTests.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/BasicTests.dot");
    }

    @Test
    public void testLoops() {
        String inputFile = "src/test/resources/Loops.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Loops.dot");
    }

    @Test
    public void testExceptions() {
        String inputFile = "src/test/resources/Exceptions.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Exceptions.dot");
    }

    @Test
    public void testNested() {
        String inputFile = "src/test/resources/Nested.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Nested.dot");
    }

    @Test
    public void testInterface() {
        String inputFile = "src/test/resources/Interface.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Interface.dot");
    }

    @Test
    public void testAssert() {
        String inputFile = "src/test/resources/Assert.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Assert.dot");
    }

    @Test
    public void testNameflow() {
        String inputFile = "src/test/resources/Calc.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "", "", "build/testTogetherTest.dot");
    }
}
