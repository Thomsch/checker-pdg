package org.checkerframework.flexeme;

import com.google.common.graph.EndpointPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * This class prints a Program Dependence Graph {@link PdgMethod} in the DOT format.
 */
public class DotPrinter {
    private static final Logger logger = LoggerFactory.getLogger(DotPrinter.class);

    /**
     * Print PDGs graphs as one dot file.
     * @param graphs The PDGs graphs from a file to print.
     */
    public String printDot(Set<PdgMethod> graphs) {
        StringBuilder stringBuilder = new StringBuilder("digraph {");
        stringBuilder.append(System.lineSeparator());
        int counter = 0;
        for (final PdgMethod graph : graphs) {
            stringBuilder.append(printGraph(graph, counter));
            stringBuilder.append(System.lineSeparator());
            counter++;
        }

        // TODO: All the edges are printed at the end (that's what C# PDG does)
        stringBuilder.append("}");
        return stringBuilder.toString();
    }


    /**
     * Print a PDG as a dot subgraph.
     * @param graph The PDG to print.
     * @param cluster The cluster number.
     */
    @SuppressWarnings("UnstableApiUsage")
    public String printGraph(PdgMethod graph, int cluster) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("subgraph " + "cluster_").append(cluster).append(" {");
        stringBuilder.append(System.lineSeparator());
        stringBuilder.append(printSubgraphLabel(graph));
        stringBuilder.append(System.lineSeparator());

        // Print nodes
        for (final PdgNode node : graph.nodes()) {
            stringBuilder.append(printNode(node));
            stringBuilder.append(System.lineSeparator());
        }

        // Print edges
        for (final EndpointPair<PdgNode> edge : graph.edges()) {
            Optional<PdgEdge.Type> edgeType = graph.edgeValue(edge);
            if (edgeType.isPresent()) {
                stringBuilder.append(printEdge(edge, edgeType.get()));
                stringBuilder.append(System.lineSeparator());
            } else {
                logger.error("Edge type is empty for edge: " + edge);
            }
        }

        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static String printEdge(final EndpointPair<PdgNode> edge, final PdgEdge.Type edgeType) {
        return String.format("n%d -> n%d [key=%d, style=%s, color=%s];", edge.source().getId(), edge.target().getId(), edgeType.getKey(), edgeType.getStyle(), edgeType.getColor());
    }

    public String printNode(final PdgNode node) {
        return String.format("n%d [label=\"%s\", span=\"%d-%d\"];", node.getId(), node.toString().replace("\"", "'"), node.getStartLine(), node.getEndLine());
    }

    public String printSubgraphLabel(final PdgMethod graph) {
        return String.format("label = \"%s.%s()\";", graph.getClassName(), graph.getMethodName());
    }
}
