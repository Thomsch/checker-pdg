package org.checkerframework.flexeme;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgEdge;
import org.checkerframework.flexeme.pdg.PdgNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Adds the control flow edges to the PDG passed in {@link #traverseEdges(MethodPdg, ControlFlowGraph)}.
 * NOTE: {@link DOTCFGVisualizer} is designed to traverse the CFG and build a DOT representation. Our implementation
 * only needs to traverse the CFG and add the edges to the PDG. The DOT representation is not needed. Unfortunately, there
 * are no implementation that just traverse the PDG.
 * TODO: Split DOTCFGVisualizer between the CFG traversal and the DOT generation.
 */
public class CfgTraverser extends DOTCFGVisualizer<VariableReference, DataflowStore, DataflowTransfer> {

    Logger logger = LoggerFactory.getLogger(CfgTraverser.class);

    private MethodPdg methodPdg;

    public CfgTraverser() {
        super();
    }

    /**
     * Main method that traverses the CFG and adds the edges to the PDG.
     *
     * @param methodPdg        the PDG
     * @param controlFlowGraph the CFG
     */
    public void traverseEdges(final MethodPdg methodPdg, final ControlFlowGraph controlFlowGraph) {
        this.methodPdg = methodPdg;

        // Traverse the blocks.
        visualizeGraphWithoutHeaderAndFooter(controlFlowGraph, controlFlowGraph.getEntryBlock(), null);
    }

    /**
     * Disabled method because we don't need to generate the DOT representation. Use {@link #traverseEdges(MethodPdg, ControlFlowGraph)} instead.
     */
    @Override
    public Map<String, Object> visualize(ControlFlowGraph cfg, Block entry, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        throw new UnsupportedOperationException("Use traverseEdges instead");
    }

    /**
     * Visualizes the nodes
     *
     * @param blocks   the set of all the blocks in a control flow graph
     * @param cfg      the control flow graph
     * @param analysis the current analysis
     * @return An empty string
     */
    @Override
    public String visualizeNodes(Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        for (final Block block : blocks) {
            // TODO If the block has no PDG elements, skip it (there won't be any "from" node to see).

            // Convert CFG edges between blocks to PDG edges between PDG nodes.
            if (block.equals(cfg.getRegularExitBlock())) { // Exit block, create edge between last statements and exit
                List<PdgNode> fromNodes = findFromNodes(block);
                List<PdgNode> toNodes = findToNodes(cfg.getEntryBlock());
                for (PdgNode from : fromNodes) {
                    for (PdgNode to : toNodes) {
                        PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.EXIT);
                        methodPdg.addEdge(edge);
                    }
                }
            } else {
                List<PdgNode> fromNodes = findFromNodes(block);

                if (fromNodes.size() == 0) {
                    logger.debug("No from node found for block: " + block);
                    continue;
                }

                List<PdgNode> toNodes = new ArrayList<>();
                for (final Block successor : block.getSuccessors()) {
                    toNodes.addAll(findToNodes(successor));
                }

                if (toNodes.size() == 0) {
                    logger.debug("No to node found for block: " + block);
                    continue;
                }

                for (PdgNode from : fromNodes) {
                    for (PdgNode to : toNodes) {
                        if (!from.equals(to)) {
                            PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CONTROL);
                            // System.out.println(edge);
                            methodPdg.addEdge(edge);
                        }
                    }
                }
            }

            // Convert CFG edges in a block to PDG edges between PDG nodes.
            Node previousNode = null;
            for (final Node node : block.getNodes()) {
                // Ignore nodes that are not in the PDG.
                if (methodPdg.getNode(node) == null) {
                    continue;
                }

                if (previousNode != null) {
                    PdgNode from = methodPdg.getNode(previousNode);
                    PdgNode to = methodPdg.getNode(node);

                    if (from == null) {
                        logger.error("No from node found for node: " + previousNode);
                    } else if (to == null) {
                        logger.error("No to node found for node: " + node);
                    } else if (!from.equals(to)) { // Skip self-edges on PDG nodes unless there is a true self loop in the CFG.
                        PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CONTROL);
                        // System.out.println(edge);
                        methodPdg.addEdge(edge);
                    }
                }
                previousNode = node;
            }
        }
        return "";
    }

    /**
     * Find the outgoing node from a given block.
     *
     * @param block the block to find the outgoing node for
     * @return the outgoing node
     */
    private List<PdgNode> findFromNodes(final Block block) {
        switch (block.getType()) {
            case REGULAR_BLOCK:
                ListIterator<Node> iterator = block.getNodes().listIterator(block.getNodes().size());
                while (iterator.hasPrevious()) {
                    Node previous = iterator.previous();
                    if (methodPdg.getNode(previous) != null) {
                        return List.of(methodPdg.getNode(previous));
                    }
                }

                List<PdgNode> predecessorNodes1 = new ArrayList<>();
                for (final Block predecessor : block.getPredecessors()) {
                    predecessorNodes1.addAll(findFromNodes(predecessor));
                }
                return predecessorNodes1;

            case CONDITIONAL_BLOCK:
                List<PdgNode> predecessorNodes2 = new ArrayList<>();
                for (final Block predecessor : block.getPredecessors()) {
                    predecessorNodes2.addAll(findFromNodes(predecessor));
                }
                return predecessorNodes2;
            case SPECIAL_BLOCK:
                return List.of(methodPdg.getNode((SpecialBlock) block));
            case EXCEPTION_BLOCK:
                ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                PdgNode node = methodPdg.getNode(exceptionBlock.getNode());
                if (node == null) {
                    return List.of();
                } else {
                    return List.of(node);
                }
            default:
                throw new IllegalStateException("Unexpected value: " + block.getType());
        }
    }

    /**
     * Find the incoming node to a given block.
     *
     * @param block the block to find the incoming node for
     * @return the incoming node
     */
    private List<PdgNode> findToNodes(final Block block) {
        switch (block.getType()) {
            case REGULAR_BLOCK:
                for (Node node : block.getNodes()) {
                    // System.out.println("    " + node);
                    if (methodPdg.getNode(node) != null) {
                        return List.of(methodPdg.getNode(node));
                    }
                }

                List<PdgNode> successorsNodes1 = new ArrayList<>();
                for (final Block successor : block.getSuccessors()) {
                    successorsNodes1.addAll(findToNodes(successor));
                }
                return successorsNodes1;

            case CONDITIONAL_BLOCK:
                List<PdgNode> successorsNodes2 = new ArrayList<>();
                for (final Block successor : block.getSuccessors()) {
                    successorsNodes2.addAll(findToNodes(successor));
                }
                return successorsNodes2;
            case SPECIAL_BLOCK:
                return List.of(methodPdg.getNode((SpecialBlock) block));
            case EXCEPTION_BLOCK:
                ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                PdgNode node = methodPdg.getNode(exceptionBlock.getNode());
                if (node == null) {
                    final List<PdgNode> successorsNodes3 = new ArrayList<>();
                    for (final Block successor : block.getSuccessors()) {
                        successorsNodes3.addAll(findToNodes(successor));
                    }
                    return successorsNodes3;
                } else {
                    return List.of(node);
                }
            default:
                throw new IllegalStateException("Unexpected value: " + block.getType());
        }
    }
}
