package tests;

import org.checkerframework.checker.codechanges.FlexemeDataflowPlayground;
import org.junit.Test;

public class DataFlowGenerationTest {
    @Test
    public void sample() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "sample";
        String clazz = "BasicTests";

        FlexemeDataflowPlayground playground = new FlexemeDataflowPlayground(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void exception() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "exceptionTest";
        String clazz = "BasicTests";

        FlexemeDataflowPlayground playground = new FlexemeDataflowPlayground(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void anonymous() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "anonymous";
        String clazz = "BasicTests";

        FlexemeDataflowPlayground playground = new FlexemeDataflowPlayground(inputFile, outputDir, method, clazz);
        playground.run();
    }
}
