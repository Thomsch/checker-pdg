package tests;

import org.checkerframework.flexeme.DotPrinter;
import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.pdg.PdgBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnonymousMethodTest {
    static FileProcessor processor;
    private PdgBuilder pdgBuilder;

    @BeforeClass
    public static void setUp() {
        PdgExtractor extractor = new PdgExtractor();
        processor = extractor.compileFile("src/test/resources/AnonymousMethods.java", "build/", false, "", "");
    }

    @Before
    public void setUpMethod() {
        pdgBuilder = new PdgBuilder();
    }

    @Test
    public void testAnonymousMethod() {
        final MethodPdg pdg = pdgBuilder.buildPdg(processor, processor.getMethod("anonymousMethod"));

        DotPrinter.printPdg(pdg);

        assertEquals(7, pdg.nodes().size());
        assertTrue(pdg.containsNode("Integer a = x"));
        assertTrue(pdg.containsNode("Optional optX = Optional.of(x)"));
        assertTrue(pdg.containsNode("optX.ifPresent(java.lang.invoke.LambdaMetafactory.metafactory())")); // optX.ifPresent(n -> System.out.println(n))
        assertTrue(pdg.containsNode("Integer b = (Integer)optX.get()")); // Integer b = optX.get()
    }
}
