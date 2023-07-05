package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

public class DataflowTest {

    @Test
    public void testDataFlow() {
        String inputFile1 = "src/test/resources/BasicTests.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.compileFile(inputFile1, "build/", false, "", "");

        String inputFile2 = "src/test/resources/DataFlow.java";

        extractor.compileFile(inputFile2, "build/", false, "", "");
    }
}
