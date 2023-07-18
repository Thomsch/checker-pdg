package tests;

import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgEdge;
import org.checkerframework.flexeme.pdg.PdgNode;
import org.junit.Assert;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * Utility class testing PDGs.
 */
public class PdgUtils {
    /**
     * Asserts that the PDG has exactly the given nodes.
     * @param expectedNodes the list of expected nodes, given as strings. The order of the nodes does not matter
     * @param actualNodes the actual nodes of the PDG
     */
    public static void assertEquals(final Set<String> expectedNodes, final Set<PdgNode> actualNodes) {
        Assert.assertEquals(expectedNodes.stream().sorted().collect(Collectors.toList()), actualNodes.stream().map(PdgNode::toString).sorted().collect(Collectors.toList()));
    }

    /**
     * Asserts that the PDG has an exact number of edges of the given type.
     * @param expectedNumberOfEdge the expected number of edges
     * @param expectedEdgeType the expected  type of the edges
     * @param actualPdg the actual PDG
     */
    public static void assertEdgeCount(final int expectedNumberOfEdge, final PdgEdge.Type expectedEdgeType, final MethodPdg actualPdg) {
        final Set<PdgEdge> dataEdges = actualPdg.edges().stream().filter(e -> e.type == expectedEdgeType).collect(Collectors.toUnmodifiableSet());
        Assert.assertEquals(expectedNumberOfEdge, dataEdges.size());
    }

    /**
     * Checks if the graph contains a control edge with the given labels.
     * @param expectedFromLabel the expected  label of the source node
     * @param expectedToLabel the expected  label of the target node
     * @param actualMethodPdg the actual PDG
     */
    public static void assertContainsEdge(final String expectedFromLabel, final String expectedToLabel, MethodPdg actualMethodPdg) {
        assertContainsEdge(expectedFromLabel, expectedToLabel, PdgEdge.Type.CONTROL, actualMethodPdg);
    }

    /**
     * Checks if the graph contains an edge with the given labels and type.
     * @param expectedFromLabel the expected label of the source node
     * @param expectedToLabel the expected  label of the target node
     * @param expectedType the expected  type of the edge
     * @param actualMethodPdg the actual PDG
     */
    public static void assertContainsEdge(final String expectedFromLabel, final String expectedToLabel, final PdgEdge.Type expectedType, final MethodPdg actualMethodPdg) {
        boolean found = false;
        for (PdgEdge edge : actualMethodPdg.edges()) {
            if (edge.from.toString().equals(expectedFromLabel)
                    && edge.to.toString().equals(expectedToLabel)
                    && edge.type.equals(expectedType)) {
                found = true;
            }
        }
        assertTrue(found);
    }

    /**
     * Checks if the graph contains a self edge with the given labels and type.
     * @param expectedFromToLabel the expected label of the source and to node
     * @param expectedType the expected  type of the edge
     * @param actualMethodPdg the actual PDG
     */
    public static void assertContainsSelfEdge(final String expectedFromToLabel, final PdgEdge.Type expectedType, final MethodPdg actualMethodPdg) {
        boolean found = false;
        for (PdgEdge edge : actualMethodPdg.edges()) {
            if (edge.from.toString().equals(expectedFromToLabel)
                    && edge.to.toString().equals(expectedFromToLabel)
                    && edge.type.equals(expectedType)) {
                found = true;
            }
        }
        assertTrue(found);
    }
}
