package tests;

import org.checkerframework.flexeme.DotPrinter;
import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgBuilder;
import org.checkerframework.flexeme.pdg.PdgEdge;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;
import static tests.PdgUtils.*;

/**
 * Test class for name flow analysis.
 */
public class NameFlowTest {
        static FileProcessor processor;
        private PdgBuilder pdgBuilder;

        @BeforeClass
        public static void setUp() {
            PdgExtractor extractor = new PdgExtractor();
            processor = extractor.compileFile("src/test/resources/NameFlow.java", "build/", false, "", "");
        }

        @Before
        public void setUpMethod() {
            pdgBuilder = new PdgBuilder();
        }

        @Test
        public void testRefinymExample() {
            final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("nameFlow"));
            DotPrinter.printPdg(methodPdg);

            assertContainsEdge("int sum = x", "Entry", PdgEdge.Type.NAME, methodPdg);
            assertContainsEdge("int snd = fst", "int fst = 20", PdgEdge.Type.NAME, methodPdg);
            // assertContainsSelfEdge("int fst = 20", PdgEdge.Type.NAME, methodPdg);

            assertContainsEdge("Entry", "int sum = x", PdgEdge.Type.NAME, methodPdg);
        }

    @Test
    public void testSampleExample() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("loopy"));
        DotPrinter.printPdg(methodPdg);

        assertContainsEdge("int a = 0", "Entry", PdgEdge.Type.NAME, methodPdg); // a is assigned to a parameter later in the code
        assertContainsEdge("int b = 100", "Entry", PdgEdge.Type.NAME, methodPdg); // b is assigned to a parameter later in the code
        // assertContainsEdge("int b = 100", "int c = 100", PdgEdge.Type.NAME, methodPdg); // Shared use of literal
        // assertContainsSelfEdge("int a = 0", PdgEdge.Type.NAME, methodPdg); // Literal 0 is there.

    }

    @Test
    public void testBar() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("bar"));
        DotPrinter.printPdg(methodPdg);

        assertEdgeCount(3, PdgEdge.Type.NAME, methodPdg);
        assertContainsEdge("Entry", "int omega = w + z", PdgEdge.Type.NAME, methodPdg);
        assertContainsEdge("int omega = w + z", "Entry", PdgEdge.Type.NAME, methodPdg); // for w
        assertContainsEdge("int omega = w + z", "Entry", PdgEdge.Type.NAME, methodPdg); // for z
    }
}
