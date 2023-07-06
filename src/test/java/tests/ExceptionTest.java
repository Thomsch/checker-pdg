package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

public class ExceptionTest {
    @Test
    public void testExceptions() {
        String inputFile = "src/test/resources/Exceptions.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Exceptions.dot");
    }
}
