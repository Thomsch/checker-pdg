package tests;

import org.checkerframework.flexeme.FileProcessor;
import org.checkerframework.flexeme.PdgExtractor;
import org.checkerframework.flexeme.pdg.FilePdg;
import org.checkerframework.flexeme.pdg.PdgBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for local method calls. For example, if a method calls another method in the same class, there should be
 * an edge between the two methods in the graph.
 */
public class LocalMethodCallsTest {
    private static FilePdg filePdg;

    @BeforeClass
    public static void setUp() {
        PdgExtractor pdgExtractor = new PdgExtractor();
        FileProcessor processor = pdgExtractor.compileFile("src/test/resources/LocalMethodCalls.java", "build/", false, "", "");
        PdgBuilder pdgBuilder = new PdgBuilder();
        filePdg = pdgBuilder.buildPdgForFile(processor);
    }

    @Test
    public void testLocalMethodCall() {
        assertTrue(filePdg.containsCall("LocalMethodCalls.local()", "int a = bar(1, 2)", "LocalMethodCalls.bar(int,int)"));
        assertTrue(filePdg.containsCall("LocalMethodCalls.local()", "int b = bar(3)", "LocalMethodCalls.bar(int)"));
    }

    @Test
    public void testChainedCall() {
        assertTrue(filePdg.containsCall("LocalMethodCalls.chained()", "int a = getThis().bar(4, 5)", "LocalMethodCalls.bar(int,int)"));
        assertTrue(filePdg.containsCall("LocalMethodCalls.chained()", "int a = getThis().bar(4, 5)", "LocalMethodCalls.getThis()"));
    }

    @Test
    public void testNestedCall() {
        assertTrue(filePdg.containsCall("LocalMethodCalls.nested()", "int a = bar(bar(6, 7), bar(8))", "LocalMethodCalls.bar(int,int)"));
        assertTrue(filePdg.containsCall("LocalMethodCalls.nested()", "int a = bar(bar(6, 7), bar(8))", "LocalMethodCalls.bar(int)"));
    }

    @Test
    public void testOnlyLocalCall() {
        assertTrue(filePdg.containsCall("LocalMethodCalls.localAndNotLocal()", "int a = bar(5)", "LocalMethodCalls.bar(int)"));
        assertFalse(filePdg.containsCall("LocalMethodCalls.localAndNotLocal()", "int b = Math.abs(a)", "Math.abs(int)"));
    }
}
