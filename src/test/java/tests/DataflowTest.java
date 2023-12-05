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

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static tests.PdgUtils.assertContainsEdge;
import static tests.PdgUtils.assertEdgeCount;

public class DataflowTest {

    static FileProcessor processor;
    private PdgBuilder pdgBuilder;

    @BeforeClass
    public static void setUp() {
        PdgExtractor extractor = new PdgExtractor();
        processor = extractor.compileFile("src/test/resources/DataFlow.java", "build/", false, "", "");
    }

    @Before
    public void setUpMethod() {
        pdgBuilder = new PdgBuilder();
    }

    @Test
    public void testParameterFlow() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("parameterFlow"));

        final Set<String> expectedNodes = Set.of("Entry", "int x = i", "String y = s", "Exit");
        PdgUtils.assertEquals(expectedNodes, methodPdg.nodes());

        assertContainsEdge("Entry", "int x = i", PdgEdge.Type.CONTROL, methodPdg);
        assertContainsEdge("Entry", "int x = i", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("Entry", "String y = s", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int x = i", "String y = s", PdgEdge.Type.CONTROL, methodPdg);
        assertContainsEdge("String y = s", "Exit", PdgEdge.Type.CONTROL, methodPdg);
    }

    /**
     * Resulting regular and exceptional exit stores should be used to create the dataflow edges.
     */
    @Test
    public void testExitStores() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("exitStores"));

        final Set<String> expectedNodes = Set.of("Entry", "int a = 1", "int b = 2", "int c = 3",
                "System.out.println(\"try\")", "int d = a", "Exception ex",  "System.out.println(\"catch\")",
                "int e = b", "System.out.println(\"finally\")", "int f = c", "int z = a + b + c",
                "Exit", "ExceptionalExit");
        PdgUtils.assertEquals(expectedNodes, methodPdg.nodes());

        assertEdgeCount(8, PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int a = 1", "int d = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int a = 1", "int z = a + b + c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int b = 2", "int e = b", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int b = 2", "int z = a + b + c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int c = 3", "int f = c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int d = a", "int z = a + b + c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int e = b", "int z = a + b + c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int f = c", "int z = a + b + c", PdgEdge.Type.DATA, methodPdg);
    }

    @Test
    public void testOverride() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("override"));

        assertEdgeCount(3, PdgEdge.Type.DATA, methodPdg);

        assertContainsEdge("int a = 1", "int b = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("a = 2", "int c = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int b = a", "a = 2", PdgEdge.Type.DATA, methodPdg); // Data flows through reassignments.
    }

    @Test
    public void testCompound() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("compound"));

        assertEdgeCount(13, PdgEdge.Type.DATA, methodPdg);

        assertContainsEdge("Entry", "c = c + a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("c = c + a", "c = c + a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("c = c + a", "System.out.println(a)", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("c = c + a", "System.out.println(c)", PdgEdge.Type.DATA, methodPdg);

        assertContainsEdge("System.out.println(a)", "c += a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("System.out.println(c)", "c += a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("c += a", "c += a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("c += a", "System.out.println(a)", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("c += a", "System.out.println(c)", PdgEdge.Type.DATA, methodPdg);

        assertContainsEdge("System.out.println(c)", "++c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("++c", "++c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("++c", "System.out.println(c)", PdgEdge.Type.DATA, methodPdg);


    }

    @Test
    public void testFieldsHaveNoFlow() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("fieldsHaveNoFlow"));

        assertEdgeCount(2, PdgEdge.Type.DATA, methodPdg);

        assertContainsEdge("int b = this.a", "int d = b + c", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int c = a", "int d = b + c", PdgEdge.Type.DATA, methodPdg);
    }

    @Test
    public void testNameConflict() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("nameConflict"));

        assertEdgeCount(2, PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int a = 0", "int c = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int c = a", "int e = a", PdgEdge.Type.DATA, methodPdg);
    }

    @Test
    public void testJoin() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("join"));

        assertEdgeCount(5, PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int a = 0", "(a == 0)", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("(a == 0)", "int b = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("(a == 0)", "int c = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int b = a", "int d = a", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int c = a", "int d = a", PdgEdge.Type.DATA, methodPdg);
    }

    @Test
    public void testLoop() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("loop"));

        assertEdgeCount(5, PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int j = 0", "j < 10", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("j < 10", "System.out.println(j)", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("System.out.println(j)", "++j", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("++j", "++j", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("++j", "j < 10", PdgEdge.Type.DATA, methodPdg);
    }

    @Test
    public void testReturnFlow() {
        final MethodPdg methodPdg = pdgBuilder.buildPdg(processor, processor.getMethod("returnFlow"));

        assertEdgeCount(1, PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int a = 0", "return a;", PdgEdge.Type.DATA, methodPdg);
        assertContainsEdge("int a = 0", "return a;", PdgEdge.Type.CONTROL, methodPdg);
    }
}
