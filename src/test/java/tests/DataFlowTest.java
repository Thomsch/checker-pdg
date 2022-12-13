package tests;

import org.checkerframework.checker.codechanges.FlexemePdgGenerator;
import org.junit.Test;

/**
 * Test that the dataflow analysis is working properly.
 */
public class DataFlowTest {
    @Test
    public void sample() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "sample";
        String clazz = "BasicTests";

        FlexemePdgGenerator playground = new FlexemePdgGenerator(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void exception() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "exceptionTest";
        String clazz = "BasicTests";

        FlexemePdgGenerator playground = new FlexemePdgGenerator(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void anonymous() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "anonymous";
        String clazz = "BasicTests";

        FlexemePdgGenerator playground = new FlexemePdgGenerator(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void loopy() {
        String inputFile = "src/test/resources/BasicTests.java";
        String outputDir = "build/tmp";
        String method = "loopy";
        String clazz = "BasicTests";

        FlexemePdgGenerator playground = new FlexemePdgGenerator(inputFile, outputDir, method, clazz);
        playground.run();
    }

    @Test
    public void join() {
        String inputFile = "src/test/resources/DataFlowTests.java";
        String outputDir = "build/tmp";
        String method = "join";
        String clazz = "DataFlowTests";

        FlexemePdgGenerator playground = new FlexemePdgGenerator(inputFile, outputDir, method, clazz);
        playground.run();
    }

    /**
     * The C# PDG generation ignore class fields so should our implementation.
     */
    @Test
    public void nameConflict() {
        String inputFile = "src/test/resources/DataFlowTests.java";
        String outputDir = "build/tmp";
        String method = "nameConflict";
        String clazz = "DataFlowTests";

        FlexemePdgGenerator playground = new FlexemePdgGenerator(inputFile, outputDir, method, clazz);
        playground.run();
    }
}
