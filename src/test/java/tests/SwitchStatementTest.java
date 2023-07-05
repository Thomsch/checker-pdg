package tests;

import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

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

        assertEquals(7, methodPdg.nodes().size());

        assertTrue(methodPdg.containsNode("int a"));
        assertTrue(methodPdg.containsNode("x")); // switch variable evaluation
        assertTrue(methodPdg.containsNode("a = 1"));
        assertTrue(methodPdg.containsNode("a = 2"));
        assertTrue(methodPdg.containsNode("a = 3"));
        assertFalse(methodPdg.containsNode("ExceptionalExit"));

        assertTrue(methodPdg.containsEdge("Entry", "int a"));
        assertTrue(methodPdg.containsEdge("int a", "x"));
        assertTrue(methodPdg.containsEdge("x", "a = 1"));
        assertTrue(methodPdg.containsEdge("x", "a = 2"));
        assertTrue(methodPdg.containsEdge("x", "a = 3"));
        assertTrue(methodPdg.containsEdge("a = 1", "Exit"));
        assertTrue(methodPdg.containsEdge("a = 2", "Exit"));
        assertTrue(methodPdg.containsEdge("a = 3", "Exit"));
        assertTrue(methodPdg.containsEdge("Exit", "Entry"));
    }

    /**
     * Test method fallThrough in class Switches.
     */
    @Test
    public void testFallThrough() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("fallThrough"));

        assertEquals(7, methodPdg.nodes().size());
        assertFalse(methodPdg.containsNode("ExceptionalExit"));

        assertTrue(methodPdg.containsEdge("Entry", "int a"));
        assertTrue(methodPdg.containsEdge("int a", "x"));
        assertTrue(methodPdg.containsEdge("x", "a = 1"));
        assertTrue(methodPdg.containsEdge("x", "a = 2"));
        assertTrue(methodPdg.containsEdge("x", "a = 3"));
        assertTrue(methodPdg.containsEdge("a = 1", "a = 2")); // fall through
        assertFalse(methodPdg.containsEdge("a = 1", "Exit")); // fall through
        assertTrue(methodPdg.containsEdge("a = 2", "Exit"));
        assertTrue(methodPdg.containsEdge("a = 3", "Exit"));
        assertTrue(methodPdg.containsEdge("Exit", "Entry"));
    }
}
