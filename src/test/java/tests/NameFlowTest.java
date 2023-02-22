package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

public class NameFlowTest {
    @Test
    public void testToyExample() {
        String inputFile = "src/test/resources/Calc.java";
        PdgExtractor.nameFlow(inputFile, "build/");
    }

    @Test
    public void testBasicTests() {
        String inputFile = "src/test/resources/BasicTests.java";
        PdgExtractor.nameFlow(inputFile, "build/");
    }

    @Test
    public void testTogetherTest() {
        String inputFile = "src/test/resources/Calc.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "", "", "build/testTogetherTest.dot");
    }
}
