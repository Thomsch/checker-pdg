package tests;

import org.checkerframework.flexeme.PdgExtractor;
import org.junit.Test;

public class NameFlowTest {
    @Test
    public void testToyExample() {
        String inputFile = "src/test/resources/Calc.java";

        PdgExtractor.nameFlow(inputFile, "build/");
    }
}
