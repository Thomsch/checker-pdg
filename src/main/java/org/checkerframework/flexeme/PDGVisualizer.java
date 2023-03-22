package org.checkerframework.flexeme;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.*;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.checkerframework.javacutil.TypesUtils;
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
public class PDGVisualizer extends DOTCFGVisualizer<VariableReference, DataflowStore, DataflowTransfer> {
    private final String cluster;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;

    private String lastStatementInBlock;
    private List<Edge> cfgEdges;

    Logger logger = LoggerFactory.getLogger(PDGVisualizer.class);

    private Map<Block, BlockFlow> statementFlowMap;
    private String graph;

    // Stores the invocations of methods. The key is the node calling the method. The value is the accessed method signature.
    public static Map<String, String> invocations = new HashMap<>();

    // Stores the methods signature and their location in the DOT graph. The key is the method's signature. The value is the node id of the START node for the method.
    public static Map<String, String> methods = new HashMap<>();
    private static Set<String> nodes = new HashSet<>();

    public String getGraph() {
        return graph;
    }

    public static Set<String> getNodes() {
        return nodes;
    }

    public PDGVisualizer(String cluster, LineMap lineMap, CompilationUnitTree compilationUnitTree) {
        super();
        this.cluster = cluster;
        this.lineMap = lineMap;
        this.compilationUnitTree = compilationUnitTree;
        this.cfgEdges = new ArrayList<>();
        this.lastStatementInBlock = null;
        this.statementFlowMap = new HashMap<>();
    }

    @Override
    public @Nullable Map<String, Object> visualize(ControlFlowGraph cfg, Block entry, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        UnderlyingAST.CFGMethod cfgMethod = (UnderlyingAST.CFGMethod) cfg.underlyingAST;
        methods.put(methodSignature(cfgMethod), "b" + entry.getUid());
        return super.visualize(cfg, entry, analysis);
    }

    @Override
    protected String visualizeGraph(ControlFlowGraph cfg, Block entry, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        graph = super.visualizeGraph(cfg, entry, analysis);
        return graph;
    }

    @Override
    protected String visualizeGraphHeader() {
        return "";
    }

    @Override
    protected String visualizeGraphFooter() {
        return "";
    }

    enum EdgeType {
        CONTROL(0, "black", "solid"), DATA(1, "darkseagreen4", "dashed"), CALL(2, "black", "dotted"), NAME(3, "darkorchid", "bold"), EXIT(0, "blue", "bold");

        private final int key;
        private final String color;
        private final String style;

        EdgeType(int key, String color, String style) {
            this.key = key;
            this.color = color;
            this.style = style;
        }

        public String getStyle() {
            return style;
        }

        public String getColor() {
            return color;
        }

        public int getKey() {
            return key;
        }
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
        private final String inNode;
        private String outNode;

        private BlockFlow(String inNode) {
            Objects.requireNonNull(inNode);
            this.inNode = inNode;
        }

        public String getInNode() {
            return inNode;
        }

        public String getOutNode() {
            return outNode;
        }

        public void setOutNode(String outNode) {
            this.outNode = outNode;
        }
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
        String dotNodes = makeStatementNodes(blocks, analysis);

        // Intra-block edges
        StringBuilder sbDotIntraEdges = makeIntraBlockDotEdges(blocks);

        // Inter-block edges
        StringBuilder sbDotInterEdges = makeInterBlockDotEdges(blocks, cfg);

        // Special edges (i.e., the blue control edge from exit->entry
        StringBuilder sbDotSpecialEdges = makeExitEntryDotEdge(cfg);

        // Dataflow edges
        StringBuilder sbDotDataflowEdges = null;
        if (analysis == null) {
            logger.error("Analysis is null");
        } else {
            sbDotDataflowEdges = makeDataflowDotEdges(cfg, analysis);
        }
        return dotNodes + lineSeparator + sbDotIntraEdges + sbDotInterEdges + sbDotSpecialEdges + sbDotDataflowEdges;
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

            sbDotDataflowEdges.append(formatPdgEdge(fromUuid, "n" + edge.getTo().getReference().getUid(), EdgeType.DATA, label));
        }
        return sbDotDataflowEdges;
    }

    private StringBuilder makeIntraBlockDotEdges(Set<Block> blocks) {
        StringBuilder sbDotIntraEdges = new StringBuilder();
        for (Block block : blocks) {
            // Make edges for this block (if applicable)
            if (block.getType() == Block.BlockType.REGULAR_BLOCK) {
                RegularBlock regularBlock = ((RegularBlock) block);
                Node from = null;
                for (Node node : regularBlock.getNodes()) {
                    if (from != null) {
                        sbDotIntraEdges.append(formatPdgEdge("n" + from.getUid(), "n" + node.getUid(), EdgeType.CONTROL));
                    }
                    from = node;
                }

            }
        }
        return sbDotIntraEdges;
    }

    private StringBuilder makeExitEntryDotEdge(ControlFlowGraph cfg) {
        StringBuilder sb = new StringBuilder();

        // TODO verify that Flexeme's extractor also doesn't add an exit edge if the method always throws an exception.
        final SpecialBlock regularExitBlock = cfg.getRegularExitBlock();
        final BlockFlow exitBlockFlow = statementFlowMap.get(regularExitBlock);

        // If a method throws an exception instead of returning, then the regular exit block is null.
        if (exitBlockFlow == null) {
            return sb;
        }

        String from = exitBlockFlow.outNode;
        String to = statementFlowMap.get(cfg.getEntryBlock()).inNode;

        sb.append(formatPdgEdge(from, to, EdgeType.EXIT));
        return sb;
    }

    private StringBuilder makeInterBlockDotEdges(Set<Block> blocks, ControlFlowGraph cfg) {

        // Build the block flow map which keeps track of which node represents the block on the dot graph.
        for (Block block : blocks) {
            switch (block.getType()){
                case REGULAR_BLOCK:
                    final BlockFlow flow = new BlockFlow("n" + block.getNodes().get(0).getUid());
                    Node lastNode = block.getLastNode();
                    if (lastNode != null) {
                        flow.setOutNode("n" + lastNode.getUid());
                    } else {
                        logger.error("Block {} has no last node", block);
                    }
                    statementFlowMap.put(block, flow);
                    break;

                case SPECIAL_BLOCK:
                    // TODO Refactor
                    final BlockFlow specialFlow = new BlockFlow("b" + block.getUid());
                    if (block.equals(cfg.getEntryBlock())) {
                        specialFlow.setOutNode("b" + block.getUid());
                    } else if (block.equals(cfg.getRegularExitBlock())) {
                        specialFlow.setOutNode("b" + block.getUid());
                    } else if (block.equals(cfg.getExceptionalExitBlock())) {
                        specialFlow.setOutNode("b" + block.getUid()); // Add Exceptional Exit -> Exit edge
                    } else {
                        logger.error("Special block {} not supported", block);
                    }
                    statementFlowMap.put(block, specialFlow);
                    break;

                case EXCEPTION_BLOCK:
                    ExceptionBlock exceptionBlock = (ExceptionBlock) block;
                    final BlockFlow exceptionFlow = new BlockFlow("n" + exceptionBlock.getNode().getUid());
                    exceptionFlow.setOutNode("n" + exceptionBlock.getNode().getUid());
                    statementFlowMap.put(exceptionBlock, exceptionFlow);
                    break;

                case CONDITIONAL_BLOCK:
                    // TODO: Assest that `block.getLastNode()` isn't null.
                    final BlockFlow conditionalFlow = new BlockFlow("n" + block.getLastNode());
                    statementFlowMap.put(block, conditionalFlow);
                    break;
            }
        }

        // Walk inter-block edge map
        StringBuilder sbDotInterEdges = new StringBuilder();
        for (Block block : blocks) {
            String from = statementFlowMap.get(block).outNode;
            switch (block.getType()) {
                case REGULAR_BLOCK:
                    RegularBlock regularBlock = ((RegularBlock) block);

                    if (regularBlock.getRegularSuccessor() instanceof ConditionalBlock) {
                        ConditionalBlock conditionalSuccessor = (ConditionalBlock) regularBlock.getRegularSuccessor();

                        sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(conditionalSuccessor.getThenSuccessor()).inNode, EdgeType.CONTROL));
                        sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(conditionalSuccessor.getElseSuccessor()).inNode, EdgeType.CONTROL));
                    } else {
                        String to = statementFlowMap.get(regularBlock.getRegularSuccessor()).inNode;
                        sbDotInterEdges.append(formatPdgEdge(from, to, EdgeType.CONTROL));
                    }
                    break;
                case CONDITIONAL_BLOCK:
                    break;
                case SPECIAL_BLOCK:
                    SpecialBlock specialBlock = ((SpecialBlock) block);

                    for (Block successor : specialBlock.getSuccessors()) {
                        sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(successor).inNode, EdgeType.CONTROL));
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

                            sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(conditionalSuccessor.getThenSuccessor()).inNode, EdgeType.CONTROL));
                            sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(conditionalSuccessor.getElseSuccessor()).inNode, EdgeType.CONTROL));
                        } else {
                            String to = statementFlowMap.get(exceptionBlock.getSuccessor()).inNode;
                            sbDotInterEdges.append(formatPdgEdge(from, to, EdgeType.CONTROL));
                        }
                    }

                    // Add control edges to exceptional execution successors
                    for (Map.Entry<TypeMirror, Set<Block>> entry : exceptionBlock.getExceptionalSuccessors().entrySet()) {
                        for (Block exception : entry.getValue()) {
                            // FIX: May create duplicates when there are multiple entries for EXCEPTIONAL_EXIT.
                            sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(exception).inNode, EdgeType.CONTROL));
                        }
                    }
                    break;
            }

            assertNoConditionalSuccessor(block);

        }
        return sbDotInterEdges;
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

    private String makeStatementNodes(Set<Block> blocks, Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        StringBuilder sbDotNodes = new StringBuilder();

        // Definition of all nodes including their labels.
        for (Block v : blocks) {
            String strBlock = visualizeBlock(v, analysis);
            sbDotNodes.append(strBlock);
        }
        sbDotNodes.append(lineSeparator);
        return sbDotNodes.toString();
    }

    private String formatPdgEdge(@NonNull final String from, @NonNull final String to, @NonNull final EdgeType type) {
        return formatPdgEdge(from, to, type, null);
    }

    private String formatPdgEdge(@NonNull final String from, @NonNull final String to, @NonNull final EdgeType type, @Nullable final String label) {
        final StringBuilder sb = new StringBuilder(from + " -> " + to + " ");

        sb.append("[");
        sb.append("key=").append(type.key);
        sb.append(", style=").append(type.style);

        if (label != null) {
            sb.append(", label=\"").append(label).append("\"");
        }

        if (type.color != "black") {
            sb.append(", color=").append(type.color);
        }
        sb.append("];");
        sb.append(lineSeparator);
        return sb.toString();
    }

    @Override
    public String visualizeBlockNode(Node t, @Nullable Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        if (t instanceof MethodAccessNode) {
            MethodAccessNode methodAccessNode = (MethodAccessNode) t;
            String signature = methodSignature(methodAccessNode);
            invocations.put("n"+ t.getUid(), signature);
        }

        if (t.getTree() == null) {
            return formatStatementNode(String.valueOf(t.getUid()), t.toString(), 0, 0);
        } else {
            JCTree jct = ((JCTree) t.getTree());
            long lineStart = lineMap.getLineNumber(jct.getStartPosition());
            long lineEnd = lineMap.getLineNumber(jct.getPreferredPosition());

            return formatStatementNode(String.valueOf(t.getUid()), t.getTree().toString(), lineStart, lineEnd);
        }
    }

    private String formatStatementNode(String uid, String label, long lineStart, long lineEnd) {
        return formatNode("n", uid, label, lineStart, lineEnd);
    }


    private String formatBlockNode(String uid, String label, long lineStart, long lineEnd) {
        return formatNode("b", uid, label, lineStart, lineEnd);
    }

    private String formatNode(String suffix, String uid, String label, long lineStart, long lineEnd) {
        final String nodeId = suffix + uid;
        // Sometimes nodes are sent to the method multiple times. Since `nodes` is a set, this is not a problem.
        nodes.add(nodeId);
        return nodeId + " [cluster=\"" + cluster + "\", label=\"" + label.replace("\"", "") + "\", span=\"" + lineStart + "-" + lineEnd + "\"];" + lineSeparator;
    }

    private void addStatementEdge(String to) {
        if (lastStatementInBlock != null) {
            Edge e = new Edge(lastStatementInBlock, to);
            logger.info("Adding new edge {}", e);
            cfgEdges.add(e);
        }
        lastStatementInBlock = to;
    }

    @Override
    public String getSeparator() {
        return "";
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return formatBlockNode(String.valueOf(sbb.getUid()), sbb.getSpecialType().name(), 0, 0);
    }

    @Override
    protected void handleSuccessorsHelper(Block cur, Set<Block> visited, Queue<Block> workList, StringBuilder sbGraph) {
        super.handleSuccessorsHelper(cur, visited, workList, sbGraph);
    }

    @Override
    protected String visualizeEdge(Object sId, Object eId, String flowRule) {
        // No need to visualize the edges between blocks.
        return "";
    }

    @Override
    public String visualizeBlockTransferInputBefore(Block bb, Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        return "";
    }

    @Override
    public String visualizeBlockTransferInputAfter(Block bb, Analysis<VariableReference, DataflowStore, DataflowTransfer> analysis) {
        return "";
    }


    private String methodSignature(UnderlyingAST.CFGMethod cfgMethod) {
        List<String> types = cfgMethod.getMethod().getParameters().stream()
                .map(variableTree -> variableTree.getType().toString())
                .collect(Collectors.toList());
        String returnType = cfgMethod.getMethod().getReturnType() == null ?
                null : cfgMethod.getMethod().getReturnType().toString();
        return methodSignature(cfgMethod.getMethod().getName(), types, returnType);
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
