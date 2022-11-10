package tests;

import org.checkerframework.checker.codechanges.FlexemeDataflowPlayground;
import org.junit.Test;

public class DataFlowGenerationTest {
    @Test
    public void sample() {
        String inputFile = "src/test/resources/Test.java";
        String outputDir = "build/tmp";
        String method = "test";
        String clazz = "Test";

        FlexemeDataflowPlayground playground = new FlexemeDataflowPlayground(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void exception() {
        String inputFile = "src/test/resources/ExceptionTest.java";
        String outputDir = "build/tmp";
        String method = "exceptionTest";
        String clazz = "Test";

        FlexemeDataflowPlayground playground = new FlexemeDataflowPlayground(inputFile, outputDir, method, clazz);
        playground.run();
    }
}
