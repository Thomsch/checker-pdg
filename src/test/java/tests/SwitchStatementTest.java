package tests;

import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.PdgGraph;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

public class SwitchStatementTest {
    static FileProcessor processor;
    @BeforeClass
    public static void setUp() throws Exception {
        processor = PdgExtractor.compileFile("src/test/resources/Switches.java", "build/", false, "", "");
    }

    private PdgExtractor pdgExtractor;
    @Before
    public void setUpMethod() throws Exception {
        pdgExtractor = new PdgExtractor();
    }

    @Test
    public void testBasicSwitch() {
        PdgGraph pdgGraph = pdgExtractor.buildPdgGraph(processor, processor.getMethod("basicSwitch"));

        assertEquals(7, pdgGraph.nodes().size());

        assertTrue(pdgGraph.containsNode("int a"));
        assertTrue(pdgGraph.containsNode("x")); // switch variable evaluation
        assertTrue(pdgGraph.containsNode("a = 1"));
        assertTrue(pdgGraph.containsNode("a = 2"));
        assertTrue(pdgGraph.containsNode("a = 3"));
        assertFalse(pdgGraph.containsNode("ExceptionalExit"));

        assertTrue(pdgGraph.containsEdge("Entry", "int a"));
        assertTrue(pdgGraph.containsEdge("int a", "x"));
        assertTrue(pdgGraph.containsEdge("x", "a = 1"));
        assertTrue(pdgGraph.containsEdge("x", "a = 2"));
        assertTrue(pdgGraph.containsEdge("x", "a = 3"));
        assertTrue(pdgGraph.containsEdge("a = 1", "Exit"));
        assertTrue(pdgGraph.containsEdge("a = 2", "Exit"));
        assertTrue(pdgGraph.containsEdge("a = 3", "Exit"));
        assertTrue(pdgGraph.containsEdge("Exit", "Entry"));
    }

    /**
     * Test method fallThrough in class Switches.
     */
    @Test
    public void testFallThrough() {
        PdgGraph pdgGraph = pdgExtractor.buildPdgGraph(processor, processor.getMethod("fallThrough"));

        assertEquals(7, pdgGraph.nodes().size());
        assertFalse(pdgGraph.containsNode("ExceptionalExit"));

        assertTrue(pdgGraph.containsEdge("Entry", "int a"));
        assertTrue(pdgGraph.containsEdge("int a", "x"));
        assertTrue(pdgGraph.containsEdge("x", "a = 1"));
        assertTrue(pdgGraph.containsEdge("x", "a = 2"));
        assertTrue(pdgGraph.containsEdge("x", "a = 3"));
        assertTrue(pdgGraph.containsEdge("a = 1", "a = 2")); // fall through
        assertFalse(pdgGraph.containsEdge("a = 1", "Exit")); // fall through
        assertTrue(pdgGraph.containsEdge("a = 2", "Exit"));
        assertTrue(pdgGraph.containsEdge("a = 3", "Exit"));
        assertTrue(pdgGraph.containsEdge("Exit", "Entry"));
    }
}
