package org.checkerframework.flexeme;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.*;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.org.plumelib.util.IdentityArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
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
    private final String cluster;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;
    private final Map<Node, Tree> nodeMap;
    private final ControlFlowGraph controlFlowGraph;
    private Set<PdgEdge> cfgEdges2;

    private String lastStatementInBlock;
    private List<Edge> cfgEdges;

    Logger logger = LoggerFactory.getLogger(CfgTraverser.class);

    private Map<Block, BlockFlow> statementFlowMap;

    // Stores the invocations of methods. The key is the node calling the method. The value is the accessed method signature.
    public static Map<String, String> invocations = new HashMap<>();

    // Stores the methods signature and their location in the DOT graph. The key is the method's signature. The value is the node id of the START node for the method.
    public static Map<String, String> methods = new HashMap<>();

    private static Set<String> nodes = new HashSet<>();
    private PdgGraph pdgGraph;

    public CfgTraverser(String cluster, LineMap lineMap, CompilationUnitTree compilationUnitTree, Map<Node, Tree> nodeMap, final ControlFlowGraph controlFlowGraph) {
        super();
        this.cluster = cluster;
        this.lineMap = lineMap;
        this.compilationUnitTree = compilationUnitTree;
        this.nodeMap = nodeMap;
        this.controlFlowGraph = controlFlowGraph;
        this.cfgEdges = new ArrayList<>();
        this.lastStatementInBlock = null;
        this.statementFlowMap = new HashMap<>();
        this.cfgEdges2 = new HashSet<>();
    }

    public Set<PdgEdge> traverseEdges(final PdgGraph pdgGraph, final ControlFlowGraph controlFlowGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgEdges2 = new HashSet<>();
        // Traverse the blocks.
        visualizeGraphWithoutHeaderAndFooter(controlFlowGraph, controlFlowGraph.getEntryBlock(), null);
        return cfgEdges2;
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
            System.out.println("Block: " + block);

            // Convert CFG edges between blocks to PDG edges between PDG nodes.
            if (block.equals(cfg.getRegularExitBlock())) { // Exit block, create edge between last statements and exit
                PdgNode from = findFromNode(block);
                PdgNode to = findToNode(cfg.getEntryBlock());
                PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.EXIT);
                pdgGraph.addEdge(edge);
            } else {
                PdgNode from = findFromNode(block);
                for (final Block successor : block.getSuccessors()) {
                    PdgNode to = findToNode(successor);

                    if (!from.equals(to)) {
                        PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CONTROL);
                        pdgGraph.addEdge(edge);
                    }
                }
            }

            // Convert CFG edges in a block to PDG edges between PDG nodes.
            Node previousNode = null;
            for (final Node node : block.getNodes()) {
                if (previousNode != null) {
                    PdgNode from = pdgGraph.getNode(previousNode);
                    PdgNode to = pdgGraph.getNode(node);

                    // System.out.println("Edge from " + previousNode + " to " + node);
                    // System.out.println("-> " + from + " to " + to);

                    // Skip self-edges on PDG nodes unless there is a true self loop in the CFG.
                    if (!from.equals(to)) {
                        PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CONTROL);
                        pdgGraph.addEdge(edge);
                    }
                    System.out.println();
                }
                previousNode = node;
            }

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
    private PdgNode findFromNode(final Block block) {
        switch (block.getType()) {
            case REGULAR_BLOCK:
                return pdgGraph.getNode(block.getLastNode());
            case CONDITIONAL_BLOCK:
                if (block.getPredecessors().size() > 1) {
                    logger.error("Conditional block has more than one predecessor");
                }
                for (final Block predecessor : block.getPredecessors()) {
                    return findFromNode(predecessor);
                }
                throw new IllegalStateException("Conditional block has no predecessor");
            case SPECIAL_BLOCK:
                return pdgGraph.getNode((SpecialBlock) block);
            case EXCEPTION_BLOCK:
                ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                return pdgGraph.getNode(exceptionBlock.getNode());
            default:
                throw new IllegalStateException("Unexpected value: " + block.getType());
        }
    }

    /**
     * Find the incoming node to a given block.
     * @param block the block to find the incoming node for
     * @return the incoming node
     */
    private PdgNode findToNode(final Block block) {
        switch (block.getType()) {
            case REGULAR_BLOCK:
                return pdgGraph.getNode(block.getNodes().get(0));
            case CONDITIONAL_BLOCK:
                if (block.getPredecessors().size() > 1) {
                    logger.error("Conditional block has more than one predecessor");
                }
                for (final Block predecessor : block.getPredecessors()) {
                    return findFromNode(predecessor);
                }
                throw new IllegalStateException("Conditional block has no predecessor");
            case SPECIAL_BLOCK:
                return pdgGraph.getNode((SpecialBlock) block);
            case EXCEPTION_BLOCK:
                ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                return pdgGraph.getNode(exceptionBlock.getNode());
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

    /**
     * Records the in and out flow node for a {@link Block}.
     * The inNode is the first node in the CFG to enter the block.
     * The outNodes are the last in the block before flowing to nodes in another block.
     */
    private class BlockFlow {
        private final Node inNode;
        private Node outNode;

        private BlockFlow(Node inNode) {
            Objects.requireNonNull(inNode);
            this.inNode = inNode;
        }

        public Node getInNode() {
            return inNode;
        }

        public Node getOutNode() {
            return outNode;
        }

        public void setOutNode(Node outNode) {
            this.outNode = outNode;
        }
    }

    private StringBuilder makeDataflowDotEdges(ControlFlowGraph cfg, Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        DataflowStore dataflowStore = analysis.getRegularExitStore();
        if (dataflowStore == null) {
            dataflowStore = analysis.getResult().getStoreAfter(cfg.getExceptionalExitBlock());
        }

        Set<org.checkerframework.flexeme.dataflow.Edge> edges = dataflowStore.getEdges();
        StringBuilder sbDotDataflowEdges = new StringBuilder();

        for (org.checkerframework.flexeme.dataflow.Edge edge : edges) {
            Node from = edge.getFrom().getReference();

            String fromUuid;
            String label = "undefined";

            if (from.getBlock() == null) { // Parameters
                fromUuid = "b" + cfg.getEntryBlock().getUid();
                TypeMirror type = from.getType();
                label = TypesUtils.getTypeElement(type).getQualifiedName().toString();
            } else { // Local Variables
                fromUuid = "n" + edge.getFrom().getReference().getUid();

                if (from instanceof LocalVariableNode) {
                    LocalVariableNode lvnFrom = ((LocalVariableNode) from);
                    label = lvnFrom.getName();
                } else if (from instanceof FieldAccessNode) {
                    FieldAccessNode fanFrom = ((FieldAccessNode) from);
                    label = fanFrom.getFieldName();
                } else if (from instanceof VariableDeclarationNode) {
                    VariableDeclarationNode vdnFrom = ((VariableDeclarationNode) from);
                    label = vdnFrom.getName();
                } else {
                    logger.error("Unsupported 'from' dataflow edge: {}", from);
                }
            }
        }
        return sbDotDataflowEdges;
    }

    private StringBuilder makeIntraBlockDotEdges(Set<Block> blocks) {
        for (Block block : blocks) {
            // Make edges for this block (if applicable)
            if (block.getType() == Block.BlockType.REGULAR_BLOCK) {
                RegularBlock regularBlock = ((RegularBlock) block);
                Node from = null;
                for (Node node : regularBlock.getNodes()) {
                    if (from != null) {
                        // cfgEdges2.add(new PdgEdge(from, node, PdgEdge.Type.CONTROL));
                    }
                    from = node;
                }

            }
        }
        return new StringBuilder();
    }

    private StringBuilder makeExitEntryDotEdge(ControlFlowGraph cfg) {
        // TODO verify that Flexeme's extractor also doesn't add an exit edge if the method always throws an exception.
        // final SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
        // final BlockFlow exitBlockFlow = statementFlowMap.get(regularExitBlock);
        //
        // // If a method throws an exception instead of returning, then the regular exit block is null.
        // if (exitBlockFlow != null) {
        //     cfgEdges2.add(new PdgEdge(exitBlockFlow.outNode, statementFlowMap.get(cfg.getEntryBlock()).inNode, PdgEdge.Type.EXIT));
        // }
        return new StringBuilder();
    }

    private StringBuilder makeInterBlockDotEdges(Set<Block> blocks, ControlFlowGraph cfg) {

        // Build the block flow map which keeps track of which node represents the block on the dot graph.
        for (Block block : blocks) {
            switch (block.getType()){
                case REGULAR_BLOCK:
                    final BlockFlow flow = new BlockFlow(block.getNodes().get(0));
                    Node lastNode = block.getLastNode();
                    if (lastNode != null) {
                        flow.setOutNode(lastNode);
                    } else {
                        logger.error("Block {} has no last node", block);
                    }
                    statementFlowMap.put(block, flow);
                    break;

                case SPECIAL_BLOCK:
                    System.out.println("SPECIAL_BLOCK: " + block);
                    // // TODO Refactor
                    // final BlockFlow specialFlow = new BlockFlow(specialNode);
                    // if (block.equals(cfg.getEntryBlock())) {
                    //     specialFlow.setOutNode(specialNode);
                    // } else if (block.equals(cfg.getRegularExitBlock())) {
                    //     block.getLastNode();
                    //     specialFlow.setOutNode(block.getNodes().get(0));
                    // } else if (block.equals(cfg.getExceptionalExitBlock())) {
                    //     specialFlow.setOutNode(block.getNodes().get(0)); // Add Exceptional Exit -> Exit edge
                    // } else {
                    //     logger.error("Special block {} not supported", block);
                    // }
                    // statementFlowMap.put(block, specialFlow);
                    break;

                case EXCEPTION_BLOCK:
                    ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                    final BlockFlow exceptionFlow = new BlockFlow(exceptionBlock.getNode());
                    exceptionFlow.setOutNode(exceptionBlock.getNode());
                    statementFlowMap.put(exceptionBlock, exceptionFlow);
                    break;

                case CONDITIONAL_BLOCK:
                    // TODO: Assest that `block.getLastNode()` isn't null.
                    final BlockFlow conditionalFlow = new BlockFlow(block.getLastNode());
                    statementFlowMap.put(block, conditionalFlow);

                    break;
            }
        }

        // Walk inter-block edge map
        for (Block block : blocks) {
            if (statementFlowMap.get(block) == null) {
                continue;
            }
            Node from = statementFlowMap.get(block).outNode;
            switch (block.getType()) {
                case REGULAR_BLOCK:
                    RegularBlock regularBlock = ((RegularBlock) block);

                    if (regularBlock.getRegularSuccessor() instanceof ConditionalBlock) {
                        ConditionalBlock conditionalSuccessor = (ConditionalBlock) regularBlock.getRegularSuccessor();
                        // cfgEdges2.add(new PdgEdge(from, statementFlowMap.get(conditionalSuccessor.getThenSuccessor()).inNode, PdgEdge.Type.CONTROL));
                        // cfgEdges2.add(new PdgEdge(from, statementFlowMap.get(conditionalSuccessor.getElseSuccessor()).inNode, PdgEdge.Type.CONTROL));
                    } else {
                        if (statementFlowMap.get(regularBlock.getRegularSuccessor()) != null) {
                            // cfgEdges2.add(new PdgEdge(from, statementFlowMap.get(regularBlock.getRegularSuccessor()).inNode, PdgEdge.Type.CONTROL));
                        } else {
                            System.out.println("Block " + block + " has " + regularBlock.getRegularSuccessor() + "as successor");
                        }
                    }
                    break;
                case CONDITIONAL_BLOCK:
                    break;
                case SPECIAL_BLOCK:
                    SpecialBlock specialBlock = ((SpecialBlock) block);

                    for (Block successor : specialBlock.getSuccessors()) {
                        // cfgEdges2.add(new PdgEdge(from, statementFlowMap.get(successor).inNode, PdgEdge.Type.CONTROL));
                    }
                    break;
                case EXCEPTION_BLOCK:
                    ExceptionBlock exceptionBlock = ((ExceptionBlock) block);

                    // Add control edge to normal execution successor

                    // When the exception block doesn't have a successor, it means it's throwing an exception and there is
                    // no successor
                    if (exceptionBlock.getSuccessor() != null) {

                        if (exceptionBlock.getSuccessor() instanceof ConditionalBlock) {
                            ConditionalBlock conditionalSuccessor = (ConditionalBlock) exceptionBlock.getSuccessor();

                            // cfgEdges2.add(new PdgEdge(statementFlowMap.get(block).outNode, statementFlowMap.get(conditionalSuccessor.getThenSuccessor()).inNode, PdgEdge.Type.CONTROL));
                            // cfgEdges2.add(new PdgEdge(statementFlowMap.get(block).outNode, statementFlowMap.get(conditionalSuccessor.getElseSuccessor()).inNode, PdgEdge.Type.CONTROL));
                        } else {
                            if (statementFlowMap.get(exceptionBlock.getSuccessor()) != null) {
                                // cfgEdges2.add(new PdgEdge(from, statementFlowMap.get(exceptionBlock.getSuccessor()).inNode, PdgEdge.Type.CONTROL));
                            } else {
                                System.out.println("Unsupported successor: " + exceptionBlock.getSuccessor() + " for block " + block + "");
                            }
                        }
                    }

                    // Add control edges to exceptional execution successors
                    // for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionBlock.getExceptionalSuccessors().entrySet()) {
                    //     for (Block exception : entry.getValue()) {
                    //         // FIX: May create duplicates when there are multiple entries for EXCEPTIONAL_EXIT.
                    //         cfgEdges2.add(new PdgEdge(from, statementFlowMap.get(exception).inNode, PdgEdge.Type.CONTROL));
                    //     }
                    // }
                    break;
            }

            assertNoConditionalSuccessor(block);

        }
        return new StringBuilder();
    }

    /**
     * Logs an error if a non RegularBlock has a ConditionalBlock as successor.
     */
    private void assertNoConditionalSuccessor(Block block) {
        if (block.getType() == Block.BlockType.CONDITIONAL_BLOCK
                || block.getType() == Block.BlockType.SPECIAL_BLOCK){
            for (Block successor : block.getSuccessors()) {
                if (successor instanceof ConditionalBlock) {
                    logger.error("Wrong assumption: Block {} of type {} has a successor that is conditional ({})", block, block.getType(), block.getSuccessors());
                }
            }
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

        if (t.getTree() != null) {
            JCTree jct = ((JCTree) t.getTree());

            // For each node in the CFG, find the corresponding node in the PDG.
            Set<Node> found = new IdentityArraySet<>();
            TreeScanner<Void, Set<Node>> scanner = new CfgNodesScanner(controlFlowGraph);
            scanner.scan(t.getTree(), found);

            System.out.println("Analyzing CFG node: '" + t + "' (" + t.getClass() + ") (uid=" + t.getUid() + ") (hash=" + t.hashCode());
            // Block: RegularBlock([omega, w, z, (w + z), omega = (w + z), omega, omega, 1, (omega + 1), omega = (omega + 1), omega, return omega])
            for (final Node node : found) {
                System.out.println("Node: " + node + "(" + node.getClass() + ") (" + node.getUid() + ") -> " + nodeMap.get(node));
            }
            System.out.println();
            found.forEach(node -> {
                if (nodeMap.containsKey(node)) {
                    nodeMap.put(t, nodeMap.get(node));
                }
            });

            if (!nodeMap.containsKey(t)) {
                // Some artificial tree nodes cannot be found in the node map because they are never encountered in the AST tree by visiting the nodes.
                // To associate every node in the CFG to a PDG node, we find a similar node to associate the CFG node to. In practice, these nodes are the same on the AST tree.
                Node node = findSimilarNode(t, t.getBlock());
                if (node != null) {
                    nodeMap.put(t, nodeMap.get(node));
                } else {
                    logger.error("Node '" + t + "'is has no linked node in the PDG.");
                }
            }
        }
        return null;
    }

    /**
     * Find another node in the block that is equal to this node.
     * @param node The node to find a similar node for.
     * @param block The block to search in.
     * @return The similar node or null if no similar node was found.
     */
    private Node findSimilarNode(final Node node, final Block block) {
        for (final Node aNode : block.getNodes()) {
            if (node.equals(aNode) && node != aNode) {
                return aNode;
            }
        }
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
