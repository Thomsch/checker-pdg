package tests;

import org.checkerframework.checker.codechanges.FlexemeDataflowPlayground;
import org.junit.Test;

public class DataFlowGenerationTest {
    @Test
    public void sample() {
        /* Configuration: change as appropriate */
        String inputFile = "src/test/resources/Test.java"; // input file name and path
        String outputDir = "build/tmp"; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        FlexemeDataflowPlayground playground = new FlexemeDataflowPlayground(inputFile, outputDir, method, clazz);
        playground.run();
    }
}
