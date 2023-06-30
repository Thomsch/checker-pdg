package tests;

import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.PdgMethod;
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
        PdgMethod pdgMethod = pdgExtractor.buildPdg(processor, processor.getMethod("basicSwitch"));

        assertEquals(7, pdgMethod.nodes().size());

        assertTrue(pdgMethod.containsNode("int a"));
        assertTrue(pdgMethod.containsNode("x")); // switch variable evaluation
        assertTrue(pdgMethod.containsNode("a = 1"));
        assertTrue(pdgMethod.containsNode("a = 2"));
        assertTrue(pdgMethod.containsNode("a = 3"));
        assertFalse(pdgMethod.containsNode("ExceptionalExit"));

        assertTrue(pdgMethod.containsEdge("Entry", "int a"));
        assertTrue(pdgMethod.containsEdge("int a", "x"));
        assertTrue(pdgMethod.containsEdge("x", "a = 1"));
        assertTrue(pdgMethod.containsEdge("x", "a = 2"));
        assertTrue(pdgMethod.containsEdge("x", "a = 3"));
        assertTrue(pdgMethod.containsEdge("a = 1", "Exit"));
        assertTrue(pdgMethod.containsEdge("a = 2", "Exit"));
        assertTrue(pdgMethod.containsEdge("a = 3", "Exit"));
        assertTrue(pdgMethod.containsEdge("Exit", "Entry"));
    }

    /**
     * Test method fallThrough in class Switches.
     */
    @Test
    public void testFallThrough() {
        PdgMethod pdgMethod = pdgExtractor.buildPdg(processor, processor.getMethod("fallThrough"));

        assertEquals(7, pdgMethod.nodes().size());
        assertFalse(pdgMethod.containsNode("ExceptionalExit"));

        assertTrue(pdgMethod.containsEdge("Entry", "int a"));
        assertTrue(pdgMethod.containsEdge("int a", "x"));
        assertTrue(pdgMethod.containsEdge("x", "a = 1"));
        assertTrue(pdgMethod.containsEdge("x", "a = 2"));
        assertTrue(pdgMethod.containsEdge("x", "a = 3"));
        assertTrue(pdgMethod.containsEdge("a = 1", "a = 2")); // fall through
        assertFalse(pdgMethod.containsEdge("a = 1", "Exit")); // fall through
        assertTrue(pdgMethod.containsEdge("a = 2", "Exit"));
        assertTrue(pdgMethod.containsEdge("a = 3", "Exit"));
        assertTrue(pdgMethod.containsEdge("Exit", "Entry"));
    }
}
