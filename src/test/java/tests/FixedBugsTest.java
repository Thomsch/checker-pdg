package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

/**
 * Test file for test cases created from bugs encountered on Defects4J bugs.
 */
public class FixedBugsTest {
    @Test
    public void testNoTermination() {
        String inputFile = "src/test/resources/DoesNotTerminate.java";

        PdgExtractor extractor = new PdgExtractor();
        extractor.run(inputFile, "src/test/resources", "src/test/resources", "build/Infinite.dot");
    }
}
