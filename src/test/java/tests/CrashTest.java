package tests;

import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.pdg.PdgBuilder;
import org.junit.Test;

/**
 * Pass / Fail tests for the PDG extractor.
 * This test class contains high-level tests that check that the PDG extractor does not crash on certain inputs.
 */
public class CrashTest {

    @Test
    public void testSourcePath() throws Throwable {

        String inputFile = "src/test/resources/org/a/A.java";
        String sourcepath = "src/test/resources";

        PdgExtractor extractor = new PdgExtractor();
        extractor.compileFile(inputFile, "build/", false, sourcepath, "");
    }

    @Test
    public void testClassPath() throws Throwable {

        String inputFile = "src/test/resources/org/C.java";
        String classpath = "src/test/resources/guava-31.1-jre.jar";

        PdgExtractor extractor = new PdgExtractor();
        extractor.compileFile(inputFile, "build/", false, "", classpath);
    }

    @Test
    public void testEnum() {
        String inputFile1 = "src/test/resources/MyEnum.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile1, "", "", "build/Enum.dot");
    }

    @Test
    public void testAllLanguageFeatures() {
        PdgExtractor pdgExtractor = new PdgExtractor();
        PdgBuilder pdgBuilder = new PdgBuilder();
        FileProcessor processor = pdgExtractor.compileFile("src/test/resources/AllLanguageFeatures.java", "build/", false, "", "");
        pdgBuilder.buildPdgForFile(processor);
    }

    @Test
    public void testAbstract() {
        String inputFile = "src/test/resources/MyAbstract.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "", "", "build/Abstract.dot");
    }

    @Test
    public void testConditional() {
        PdgExtractor pdgExtractor = new PdgExtractor();
        PdgBuilder pdgBuilder = new PdgBuilder();
        FileProcessor processor = pdgExtractor.compileFile("src/test/resources/Conditional.java", "build/", false, "", "");
        pdgBuilder.buildPdgForFile(processor);
    }

    @Test
    public void testHangingAnalysis() {
        String inputFile = "src/test/resources/DoesNotTerminate.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Infinite.dot");
    }

    @Test
    public void testLoops() {
        String inputFile = "src/test/resources/Loops.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Loops.dot");
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
