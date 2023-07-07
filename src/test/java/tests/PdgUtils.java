package tests;

import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgEdge;
import org.checkerframework.flexeme.pdg.PdgNode;
import org.junit.Assert;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public class PdgUtils {
    /**
     * Asserts that the PDG has exactly the given nodes.
     * @param expectedNodes The list of expected nodes, given as strings. The order of the nodes does not matter.
     * @param actualNodes The actual nodes of the PDG.
     */
    public static void assertEquals(final Set<String> expectedNodes, final Set<PdgNode> actualNodes) {
        Assert.assertEquals(expectedNodes.stream().sorted().collect(Collectors.toList()), actualNodes.stream().map(PdgNode::toString).sorted().collect(Collectors.toList()));
    }

    public static void assertEdgeCount(final int expectedNumberOfEdge, final PdgEdge.Type expectedEdgeType, final MethodPdg actualPdg) {
        final Set<PdgEdge> dataEdges = actualPdg.edges().stream().filter(e -> e.type == expectedEdgeType).collect(Collectors.toUnmodifiableSet());
        Assert.assertEquals(expectedNumberOfEdge, dataEdges.size());
    }

    /**
     * Checks if the graph contains an edge with the given labels.
     * @param expectedFromLabel the label of the source node
     * @param expectedToLabel the label of the target node
     * @return true if the graph contains an edge with the given labels, false otherwise
     */
    public static void assertContainsEdge(final String expectedFromLabel, final String expectedToLabel, MethodPdg actualMethodPdg) {
        assertContainsEdge(expectedFromLabel, expectedToLabel, PdgEdge.Type.CONTROL, actualMethodPdg);
    }

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
}
