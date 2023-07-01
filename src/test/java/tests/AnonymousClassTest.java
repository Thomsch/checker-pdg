package tests;

import org.checkerframework.flexeme.DotPrinter;
import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.PdgMethod;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnonymousClassTest {

    static FileProcessor processor;
    @BeforeClass
    public static void setUp() {
        processor = PdgExtractor.compileFile("src/test/resources/AnonymousClass.java", "build/", false, "", "");
    }

    private PdgExtractor pdgExtractor;
    @Before
    public void setUpMethod() {
        pdgExtractor = new PdgExtractor();
    }

    @Test
    public void testAnonymousClass() {
        final PdgMethod pdg = pdgExtractor.buildPdg(processor, processor.getMethod("anonymousClass"));

        assertEquals(6, pdg.nodes().size());
        assertTrue(pdg.containsNode("int a = 0"));
        assertTrue(pdg.containsNode("Runnable r = new AnonymousClass$1(this, a)"));
        assertTrue(pdg.containsNode("r.run()"));
    }
}
