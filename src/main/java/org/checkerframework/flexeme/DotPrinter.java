package org.checkerframework.flexeme;

import com.google.common.graph.EndpointPair;
import org.checkerframework.flexeme.pdg.FilePdg;
import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgEdge;
import org.checkerframework.flexeme.pdg.PdgNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * This class prints a Program Dependence Graph {@link MethodPdg} in the DOT format.
 */
public class DotPrinter {
    private static final Logger logger = LoggerFactory.getLogger(DotPrinter.class);

    public static void printPdg(final MethodPdg pdg) {
        final String printedPdg = new DotPrinter().printDot(new FilePdg(Set.of(pdg), Set.of()));
        System.out.println(printedPdg);
    }

    /**
     * Convenience method to print the PDGs of a file.
     *
     * @param filePdg The PDGs to print.
     */
    public static void printFilePdg(final FilePdg filePdg) {
        final String printedPdg = new DotPrinter().printDot(filePdg);
        System.out.println(printedPdg);
    }

    /**
     * Print PDGs graphs as one dot file.
     *
     * @param filePdg The PDGs to print.
     * @return The dot file as a string.
     */
    public String printDot(FilePdg filePdg) {
        final StringBuilder stringBuilder = new StringBuilder("digraph {");
        stringBuilder.append(System.lineSeparator());
        int counter = 0;
        for (final MethodPdg graph : filePdg.getGraphs()) {
            stringBuilder.append(printGraph(graph, counter));
            stringBuilder.append(System.lineSeparator());
            counter++;
        }

        // Print local method calls
        for (final PdgEdge localCall : filePdg.getLocalCalls()) {
            stringBuilder.append(printEdge(localCall));
            stringBuilder.append(System.lineSeparator());
        }

        stringBuilder.append("}");
        return stringBuilder.toString();
    }


    /**
     * Print a PDG as a dot subgraph.
     * @param graph The PDG to print.
     * @param cluster The cluster number.
     */
    @SuppressWarnings("UnstableApiUsage")
    public String printGraph(MethodPdg graph, int cluster) {
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
        for (final PdgEdge edge : graph.edges()) {
            stringBuilder.append(printEdge(edge));
            stringBuilder.append(System.lineSeparator());
        }

        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public static String printEdge(final PdgEdge edge) {
        return String.format("n%d -> n%d [key=%d, style=%s, color=%s];", edge.from.getId(), edge.to.getId(), edge.type.getKey(), edge.type.getStyle(), edge.type.getColor());
    }

    public String printNode(final PdgNode node) {
        return String.format("n%d [label=\"%s\", span=\"%d-%d\"];", node.getId(), node.toString().replace("\"", "'"), node.getStartLine(), node.getEndLine());
    }

    public String printSubgraphLabel(final MethodPdg graph) {
        return String.format("label = \"%s.%s()\";", graph.getClassName(), graph.getMethodName());
    }
}
