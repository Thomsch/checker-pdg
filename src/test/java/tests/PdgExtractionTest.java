package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

public class PdgExtractionTest {

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
}
