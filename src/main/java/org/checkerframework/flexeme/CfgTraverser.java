package org.checkerframework.flexeme;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Name;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Visualizes the control flow graph and dataflow of a method.
 *
 * TODO: {@link DOTCFGVisualizer} is not really designed to visualize at the statement level, so the implementation below is a bit hacky.
 * This should be refactored to work in two steps:
 *  1) Extract nodes and edges from the dataflow analysis and CFG at the statement level from the block traversal below (or from one of the CheckerFramework classes)
 *  2) Run through the graph and visualize it, not using the DOTCFGVisualizer.
 */
public class CfgTraverser extends DOTCFGVisualizer<VariableReference, DataflowStore, DataflowTransfer> {
    private final CompilationUnitTree compilationUnitTree;
    private final Map<Node, Tree> cfgNodeToPdgElementMap;
    private final ControlFlowGraph controlFlowGraph;

    Logger logger = LoggerFactory.getLogger(CfgTraverser.class);

    // Stores the invocations of methods. The key is the node calling the method. The value is the accessed method signature.
    public static Map<String, String> invocations = new HashMap<>();

    // Stores the methods signature and their location in the DOT graph. The key is the method's signature. The value is the node id of the START node for the method.
    public static Map<String, String> methods = new HashMap<>();

    private PdgGraph pdgGraph;

    public CfgTraverser(CompilationUnitTree compilationUnitTree, Map<Node, Tree> cfgNodeToPdgElementMap, final ControlFlowGraph controlFlowGraph) {
        super();
        this.compilationUnitTree = compilationUnitTree;
        this.cfgNodeToPdgElementMap = cfgNodeToPdgElementMap;
        this.controlFlowGraph = controlFlowGraph;
    }

    public void traverseEdges(final PdgGraph pdgGraph, final ControlFlowGraph controlFlowGraph) {
        this.pdgGraph = pdgGraph;
        // Traverse the blocks.
        visualizeGraphWithoutHeaderAndFooter(controlFlowGraph, controlFlowGraph.getEntryBlock(), null);
    }

    /**
     * Visualizes the nodes
     *
     * @param blocks   the set of all the blocks in a control flow graph
     * @param cfg      the control flow graph
     * @param analysis the current analysis
     * @return
     */
    @Override
    public String visualizeNodes(Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        makeStatementNodes(blocks, analysis);

        for (final Block block : blocks) {
            System.out.println("BLOCK: " + block);
            System.out.println("--------------------");

            // TODO If the block has no PDG elements, skip it (there won't be any "from" node to see).

            // Convert CFG edges between blocks to PDG edges between PDG nodes.
            if (block.equals(cfg.getRegularExitBlock())) { // Exit block, create edge between last statements and exit
                List<PdgNode> fromNodes = findFromNodes(block);
                List<PdgNode> toNodes = findToNodes(cfg.getEntryBlock());
                for (PdgNode from : fromNodes) {
                    for (PdgNode to : toNodes) {
                        PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.EXIT);
                        pdgGraph.addEdge(edge);
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

                for(PdgNode from : fromNodes) {
                    for(PdgNode to : toNodes) {
                        if (!from.equals(to)) {
                            PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CONTROL);
                            System.out.println(edge);
                            pdgGraph.addEdge(edge);
                        }
                    }
                }
            }

            // Convert CFG edges in a block to PDG edges between PDG nodes.
            Node previousNode = null;
            for (final Node node : block.getNodes()) {
                // Ignore nodes that are not in the PDG.
                if (pdgGraph.getNode(node) == null) {
                    continue;
                }

                if (previousNode != null) {
                    PdgNode from = pdgGraph.getNode(previousNode);
                    PdgNode to = pdgGraph.getNode(node);

                    if (from == null) {
                        logger.error("No from node found for node: " + previousNode);
                    } else if (to == null) {
                        logger.error("No to node found for node: " + node);
                    } else if (!from.equals(to)) { // Skip self-edges on PDG nodes unless there is a true self loop in the CFG.
                        PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CONTROL);
                        System.out.println(edge);
                        pdgGraph.addEdge(edge);
                    }
                }
                previousNode = node;
            }

            System.out.println();

        }

        // Dataflow edges
        // StringBuilder sbDotDataflowEdges = null;
        // if (analysis == null) {
        //     logger.error("Analysis is null");
        // } else {
        //     sbDotDataflowEdges = makeDataflowDotEdges(cfg, analysis);
        // }
        return "";
    }

    /**
     * Find the outgoing node from a given block.
     * @param block the block to find the outgoing node for
     * @return the outgoing node
     */
    private List<PdgNode> findFromNodes(final Block block) {
        switch (block.getType()) {
            case REGULAR_BLOCK:
                ListIterator<Node> iterator = block.getNodes().listIterator(block.getNodes().size());
                while(iterator.hasPrevious()) {
                    Node previous = iterator.previous();
                    if (pdgGraph.getNode(previous) != null) {
                        return List.of(pdgGraph.getNode(previous));
                    }
                }

                System.out.println("Second pass, visiting predecessors of " + block);
                List<PdgNode> predecessorNodes1 = new ArrayList<>();
                for (final Block predecessor : block.getPredecessors()) {
                    System.out.println("    " + predecessor);
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
                return List.of(pdgGraph.getNode((SpecialBlock) block));
            case EXCEPTION_BLOCK:
                ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                PdgNode node = pdgGraph.getNode(exceptionBlock.getNode());
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
     * @param block the block to find the incoming node for
     * @return the incoming node
     */
    private List<PdgNode> findToNodes(final Block block) {
        switch (block.getType()) {
            case REGULAR_BLOCK:
                System.out.println("First pass, visiting nodes of " + block);
                for (Node node : block.getNodes()) {
                    System.out.println("    " + node);
                    if (pdgGraph.getNode(node) != null) {
                        return List.of(pdgGraph.getNode(node));
                    }
                }

                System.out.println("Second pass, visiting successors of " + block);

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
                return List.of(pdgGraph.getNode((SpecialBlock) block));
            case EXCEPTION_BLOCK:
                ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                PdgNode node = pdgGraph.getNode(exceptionBlock.getNode());
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

    @Override
    public Map<String, Object> visualize(ControlFlowGraph cfg, Block entry, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        throw new UnsupportedOperationException("Use traverseEdges instead");
    }

    private class Edge {
        private final String from;
        private final String to;

        public Edge(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "from='" + from + '\'' +
                    ", to='" + to + '\'' +
                    '}';
        }
    }

    private void makeStatementNodes(Set<Block> blocks, Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        // Definition of all nodes including their labels.
        for (Block v : blocks) {
            String strBlock = visualizeBlock(v, analysis);
        }
    }

    @Override
    public String visualizeBlockNode(Node t, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        if (t instanceof MethodAccessNode) {
            MethodAccessNode methodAccessNode = (MethodAccessNode) t;
            String signature = methodSignature(methodAccessNode);
            invocations.put("n"+ t.getUid(), signature);
        }

        // This method adds more nodes to the nodeMap.

        // TODO Map artificial variable declaration with references using the name.

        // System.out.println("Analyzing CFG node: '" + t + "' (" + t.getClass() + ") (uid=" + t.getUid() + ") (hash=" + t.hashCode() + ")");
        // System.out.println("    In node map: " + cfgNodeToPdgElementMap.containsKey(t));
        // System.out.println("    In source: " + t.getInSource());
        // if (!t.getInSource()) {
        //     System.out.println("    Artificial node in source");
        //     // TODO Refactor this to use a visitor to get the name.
        //     if (t instanceof VariableDeclarationNode) {
        //         VariableDeclarationNode variableDeclarationNode = (VariableDeclarationNode) t;
        //
        //         if (artificialNodeMap.containsKey(variableDeclarationNode.getName())) {
        //             System.out.println("    Artificial node already in map: " + artificialNodeMap.get(variableDeclarationNode.getName()));
        //             artificialNodeMap.get(variableDeclarationNode.getName()).add(variableDeclarationNode);
        //         } else {
        //             System.out.println("    Adding artificial node to map: " + variableDeclarationNode);
        //             List<Node> nodes = new ArrayList<>();
        //             nodes.add(variableDeclarationNode);
        //             artificialNodeMap.put(variableDeclarationNode.getName(), nodes);
        //         }
        //     } else if (t instanceof LocalVariableNode) {
        //         LocalVariableNode localVariableNode = (LocalVariableNode) t;
        //
        //         if (artificialNodeMap.containsKey(localVariableNode.getName())) {
        //             System.out.println("    Artificial node already in map: " + artificialNodeMap.get(localVariableNode.getName()));
        //             artificialNodeMap.get(localVariableNode.getName()).add(localVariableNode);
        //         } else {
        //             System.out.println("    Adding artificial node to map: " + localVariableNode);
        //             List<Node> nodes = new ArrayList<>();
        //             nodes.add(localVariableNode);
        //             artificialNodeMap.put(localVariableNode.getName(), nodes);
        //         }
        //     } else {
        //         System.out.println("    Unsupported artificial node: " + t + " (" + t.getClass() + ")");
        //     }
        // }
        //
        // if (t.getTree() != null) {
        //     // Find all the nodes in the AST tree that are related to the CFG node.
        //     Set<Node> found = new IdentityArraySet<>();
        //     TreeScanner<Void, Set<Node>> scanner = new CfgNodesScanner(controlFlowGraph);
        //     scanner.scan(t.getTree(), found);
        //
        //     if (cfgNodeToPdgElementMap.containsKey(t)) {
        //         // Update descendants to point to t
        //         for (final Node node : found) {
        //             // System.out.println("- related node: " + node + "(" + node.getClass() + ") (" + node.getUid() + ") -> " + nodeMap.get(node));
        //             System.out.println("Update descendant node " + node + "(" + node.getClass() + ") (" + node.getUid() + ") " + " to node " + t + " node map -> " + cfgNodeToPdgElementMap.get(node));
        //             cfgNodeToPdgElementMap.put(node, cfgNodeToPdgElementMap.get(t));
        //         }
        //     } else { // Try to find a mapped node in its descendants.
        //         final Set<Node> mappedNodes = new IdentityArraySet<>();
        //         for (final Node node : found) {
        //             if (cfgNodeToPdgElementMap.containsKey(node)) {
        //                 mappedNodes.add(node);
        //             }
        //         }
        //         if (mappedNodes.size() > 1) {
        //             // If this ever happen, we log a warning for debug purposes.
        //             logger.warn("Competing nodes found for CFG node " + t + ": " + mappedNodes);
        //         }
        //
        //         if (mappedNodes.size() == 0) {
        //             // If no mapped node is found, we log a warning for debug purposes.
        //             logger.warn("No mapped node found for CFG node " + t);
        //             if (t instanceof VariableDeclarationNode) {
        //                 VariableDeclarationNode variableDeclarationNode = (VariableDeclarationNode) t;
        //                 artificialNodeMap.get(variableDeclarationNode.getName()).forEach(artificialNode -> {
        //                     System.out.println("    Artificial node: " + artificialNode + " -> " + cfgNodeToPdgElementMap.get(artificialNode));
        //                 });
        //             } else if (t instanceof LocalVariableNode) {
        //                 LocalVariableNode localVariableNode = (LocalVariableNode) t;
        //                 artificialNodeMap.get(localVariableNode.getName()).forEach(artificialNode -> {
        //                     System.out.println("    Artificial node: " + artificialNode + " -> " + cfgNodeToPdgElementMap.get(artificialNode));
        //                 });
        //             }
        //         }
        //
        //         // Update t with the mapped node.
        //         mappedNodes.forEach(mappedNode -> {
        //             System.out.println("Update t node " + mappedNode + "(" + mappedNode.getClass() + ") (" + mappedNode.getUid() + ") " + " with mapped node " + mappedNode + " -> " + cfgNodeToPdgElementMap.get(mappedNode));
        //             cfgNodeToPdgElementMap.put(t, cfgNodeToPdgElementMap.get(mappedNode));
        //         });
        //
        //         // Update all other nodes with the mapped node.
        //         mappedNodes.forEach(mappedNode -> {
        //             found.forEach(node -> {
        //                 if (cfgNodeToPdgElementMap.containsKey(node)) {
        //                     System.out.println("Updating descendant node " + node + "(" + node.getClass() + ") (" + node.getUid() + ") with mapped node " + mappedNode + " -> " + cfgNodeToPdgElementMap.get(node));
        //                     cfgNodeToPdgElementMap.put(node, cfgNodeToPdgElementMap.get(mappedNode));
        //                 }
        //             });
        //         });
        //     }
        //
        //     if (!cfgNodeToPdgElementMap.containsKey(t)) {
        //         // Some artificial tree nodes cannot be found in the node map because they are never encountered in the AST tree by visiting the nodes.
        //         // To associate every node in the CFG to a PDG node, we find a similar node to associate the CFG node to. In practice, these nodes are the same on the AST tree.
        //         Node node = findSimilarNode(t, t.getBlock());
        //         if (node != null) {
        //             System.out.println("Found similar node: " + node + "(" + node.getClass() + ") (" + node.getUid() + ") -> " + cfgNodeToPdgElementMap.get(node));
        //             cfgNodeToPdgElementMap.put(t, cfgNodeToPdgElementMap.get(node));
        //         } else {
        //             logger.error("Node '" + t + "'is has no linked node in the PDG.");
        //         }
        //     }
        //     System.out.println();
        // } else {
        //     logger.warn("Node '" + t + "' has no tree.");
        // }
        return null;
    }

    private String methodSignature(MethodAccessNode methodAccessNode) {
        List<String> types = methodAccessNode.getMethod().getParameters().stream()
                .map(parameter -> parameter.asType().toString())
                .collect(Collectors.toList());
        return methodSignature(
                methodAccessNode.getMethod().getSimpleName(),
                types,
                methodAccessNode.getMethod().getReturnType().toString());
    }

    private String methodSignature(Name name, List<String> types, String returnType) {
        return String.format("%s %s -> %s", name, String.join(",", types), returnType == null ? "null": returnType);
    }
}
