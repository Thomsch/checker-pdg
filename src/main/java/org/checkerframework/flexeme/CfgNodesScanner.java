package org.checkerframework.flexeme;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.Collection;
import java.util.Set;

/**
 * A scanner that retrieves all the nodes corresponding to a tree.
 */
class CfgNodesScanner extends TreeScanner<Void, Set<Node>> {
    private final ControlFlowGraph cfg;

    public CfgNodesScanner(final ControlFlowGraph cfg) {
        this.cfg = cfg;
    }

    @Override
    public Void scan(final Tree tree, final Set<Node> found) {
        if (tree != null) {
            final Set<Node> nodes = cfg.getNodesCorrespondingToTree(tree);

            // System.out.println("Tree: " + tree + " " + tree.getClass());
            if (nodes != null) {
                // System.out.println("Nodes: " + nodes);
                found.addAll(nodes);
                for (final Node node : nodes) {
                    final Collection<Node> transitiveOperands = node.getTransitiveOperands();
                    found.addAll(transitiveOperands);
                    // System.out.println("   Transitive:" + transitiveOperands);
                }
            } else {
                // System.out.println("No nodes for tree");
            }
        }
        return super.scan(tree, found);
    }
}
