package org.checkerframework.checker.codechanges;

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
import org.checkerframework.javacutil.TypesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.type.TypeMirror;
import java.util.*;

public class FlexemePDGVisualizer extends DOTCFGVisualizer<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> {
    private final String cluster;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;

    private String lastStatementInBlock;
    private List<Edge> cfgEdges;

    Logger logger = LoggerFactory.getLogger(FlexemePDGVisualizer.class);

    private Map<Block, BlockFlow> statementFlowMap;
    private String graph;

    public static Map<String, String> invocations = new HashMap<>();

    public static Map<String, String> methods = new HashMap<>(); // maps from method name to entry block number.

    public String getGraph() {
        return graph;
    }

    public FlexemePDGVisualizer(String cluster, LineMap lineMap, CompilationUnitTree compilationUnitTree) {
        super();
        this.cluster = cluster;
        this.lineMap = lineMap;
        this.compilationUnitTree = compilationUnitTree;
        this.cfgEdges = new ArrayList<>();
        this.lastStatementInBlock = null;
        this.statementFlowMap = new HashMap<>();
    }

    @Override
    public @Nullable Map<String, Object> visualize(ControlFlowGraph cfg, Block entry, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        UnderlyingAST.CFGMethod cfgMethod = (UnderlyingAST.CFGMethod) cfg.underlyingAST;
        methods.put(cfgMethod.getMethodName(), "b" + entry.getUid());

//        TreeUtils.getMethod() Try this for multiple methods
//        cfgMethod.getSimpleClassName()
        return super.visualize(cfg, entry, analysis);
    }

    @Override
    protected String visualizeGraph(ControlFlowGraph cfg, Block entry, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
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

        private int key;
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
    public String visualizeNodes(Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
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

    private StringBuilder makeDataflowDotEdges(ControlFlowGraph cfg, Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        FlexemeDataflowStore dataflowStore = analysis.getResult().getStoreAfter(cfg.getRegularExitBlock());
        Set<org.checkerframework.checker.codechanges.Edge> edges = dataflowStore.getEdges();
        StringBuilder sbDotDataflowEdges = new StringBuilder();

        for (org.checkerframework.checker.codechanges.Edge edge : edges) {
            Node from = edge.getFrom().reference;

            String fromUuid;
            String label = "undefined";

            if (from.getBlock() == null) { // Parameters
                fromUuid = "b" + cfg.getEntryBlock().getUid();
                TypeMirror type = from.getType();
                label = TypesUtils.getTypeElement(type).getQualifiedName().toString();
            } else { // Local Variables
                fromUuid = "n" + edge.getFrom().reference.getUid();

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

            sbDotDataflowEdges.append(formatPdgEdge(fromUuid, "n" + edge.getTo().reference.getUid(), EdgeType.DATA, label));
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

        String from = statementFlowMap.get(cfg.getRegularExitBlock()).outNode;
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
                    sbDotInterEdges.append(formatPdgEdge(from, statementFlowMap.get(exceptionBlock.getSuccessor()).inNode, EdgeType.CONTROL));

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
                || block.getType() == Block.BlockType.EXCEPTION_BLOCK
                || block.getType() == Block.BlockType.SPECIAL_BLOCK){
            for (Block successor : block.getSuccessors()) {
                if (successor instanceof ConditionalBlock) {
                    logger.error("Wrong assumption: Block {} of type {} has a successor that is conditional ({})", block, block.getType(), block.getSuccessors());
                }
            }
        }
    }

    private String makeStatementNodes(Set<Block> blocks, Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
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
    public String visualizeBlockNode(Node t, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        if (t instanceof MethodAccessNode) {
            MethodAccessNode methodAccessNode = (MethodAccessNode) t;
            System.out.println("Call to " + methodAccessNode.getMethod());
            invocations.put("n"+ t.getUid(), methodAccessNode.getMethod().getSimpleName().toString());
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
        return suffix + uid + " [cluster=\"" + cluster + "\", label=\"" + label + "\", span=\"" + lineStart + "-" + lineEnd + "\"];" + lineSeparator;
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
    public String visualizeBlockTransferInputBefore(Block bb, Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        return "";
    }

    @Override
    public String visualizeBlockTransferInputAfter(Block bb, Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        return "";
    }
}
