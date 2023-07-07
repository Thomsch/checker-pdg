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

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;
import static tests.PdgUtils.assertContainsEdge;
import static tests.PdgUtils.assertEdgeCount;

public class SwitchStatementTest {
    static FileProcessor processor;
    private PdgBuilder pdgBuilder;

    @BeforeClass
    public static void setUp() {
        PdgExtractor extractor = new PdgExtractor();
        processor = extractor.compileFile("src/test/resources/Switches.java", "build/", false, "", "");
    }

    @Before
    public void setUpMethod() {
        pdgBuilder = new PdgBuilder();
    }

    @Test
    public void testBasicSwitch() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("basicSwitch"));

        DotPrinter.printPdg(methodPdg);
        assertEquals(7, methodPdg.nodes().size());

        assertTrue(methodPdg.containsNode("int a"));
        assertTrue(methodPdg.containsNode("(x)")); // switch variable evaluation
        assertTrue(methodPdg.containsNode("a = 1"));
        assertTrue(methodPdg.containsNode("a = 2"));
        assertTrue(methodPdg.containsNode("a = 3"));
        assertFalse(methodPdg.containsNode("ExceptionalExit"));

        assertContainsEdge("Entry", "int a", methodPdg);
        assertContainsEdge("int a", "(x)", methodPdg);
        assertContainsEdge("(x)", "a = 1", methodPdg);
        assertContainsEdge("(x)", "a = 2", methodPdg);
        assertContainsEdge("(x)", "a = 3", methodPdg);
        assertContainsEdge("a = 1", "Exit", methodPdg);
        assertContainsEdge("a = 2", "Exit", methodPdg);
        assertContainsEdge("a = 3", "Exit", methodPdg);
        assertContainsEdge("Exit", "Entry", PdgEdge.Type.EXIT, methodPdg);
    }

    /**
     * Test method fallThrough in class Switches.
     */
    @Test
    public void testFallThrough() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("fallThrough"));

        assertEquals(7, methodPdg.nodes().size());
        assertFalse(methodPdg.containsNode("ExceptionalExit"));

        assertEdgeCount(17, PdgEdge.Type.CONTROL, methodPdg);
        assertContainsEdge("Entry", "int a", methodPdg);
        assertContainsEdge("int a", "(x)", methodPdg);
        assertContainsEdge("(x)", "a = 1", methodPdg);
        assertContainsEdge("(x)", "a = 2", methodPdg);
        assertContainsEdge("(x)", "a = 3", methodPdg);
        assertContainsEdge("a = 1", "a = 2", methodPdg); // fall through
        assertContainsEdge("a = 2", "Exit", methodPdg);
        assertContainsEdge("a = 3", "Exit", methodPdg);
        assertContainsEdge("Exit", "Entry", PdgEdge.Type.EXIT,methodPdg);
    }
}
