package org.checkerframework.checker.codechanges;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.Element;
import java.util.*;

public class FlexemePDGVisualizer extends DOTCFGVisualizer<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> {
    private final String cluster;
    private final LineMap lineMap;
    private final CompilationUnitTree compilationUnitTree;

    private String lastStatementInBlock;
    private List<Edge> cfgEdges;

    private Map<Block, BlockFlow> statementFlowMap;

    public FlexemePDGVisualizer(String cluster, LineMap lineMap, CompilationUnitTree compilationUnitTree) {
        super();
        this.cluster = cluster;
        this.lineMap = lineMap;
        this.compilationUnitTree = compilationUnitTree;
        this.cfgEdges = new ArrayList<>();
        this.lastStatementInBlock = null;
        this.statementFlowMap = new HashMap<>();
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

    @Override
    public String visualizeNodes(Set<Block> blocks, ControlFlowGraph cfg, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        StringBuilder sbDotNodes = new StringBuilder();
        IdentityHashMap<Block, List<Integer>> processOrder = getProcessOrder(cfg);

        // Definition of all nodes including their labels.
        for (Block v : blocks) {

//            sbDotNodes.append("    ").append(v.getUid()).append(" [");
//            if (v.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
//                sbDotNodes.append("shape=polygon sides=8 ");
//            } else if (v.getType() == Block.BlockType.SPECIAL_BLOCK) {
//                sbDotNodes.append("shape=oval ");
//            } else {
//                sbDotNodes.append("shape=rectangle ");
//            }
//            sbDotNodes.append("label=\"");
//            if (verbose) {
//                sbDotNodes.append(getProcessOrderSimpleString(processOrder.get(v))).append(getSeparator());
//            }

            String strBlock = visualizeBlock(v, analysis);
//            if (strBlock.length() == 0) {
//                if (v.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
//                    // The footer of the conditional block.
//                    sbDotNodes.append("\"];");
//                } else {
//                    // The footer of the block which has no content and is not a special or conditional block.
//                    sbDotNodes.append("?? empty ??\"];");
//                }
//            } else {
//                sbDotNodes.append(strBlock).append("\"];");
//            sbDotNodes.append("A" + System.lineSeparator());
            sbDotNodes.append(strBlock);
//            sbDotNodes.append(System.lineSeparator() + "B");
//            }
//            sbDotNodes.append(System.lineSeparator());
            lastStatementInBlock = null;
        }
        sbDotNodes.append(lineSeparator);

        BlockFlow entryFlow = new BlockFlow(null);
        entryFlow.setOutNode("n" + cfg.getEntryBlock().getUid());
        statementFlowMap.putIfAbsent(cfg.getEntryBlock(), entryFlow);
        BlockFlow exitFlow = new BlockFlow("n" + cfg.getRegularExitBlock().getUid());
        exitFlow.setOutNode("n" + cfg.getEntryBlock().getUid()); // Add Exit -> Entry edge
        statementFlowMap.putIfAbsent(cfg.getRegularExitBlock(), exitFlow);

        sbDotNodes.append(makePdgEdge(exitFlow.inNode, exitFlow.outNode, EdgeType.EXIT));
        sbDotNodes.append(lineSeparator);

        for (Block v : blocks) {
            System.out.println(v);
            BlockFlow blockFlow = statementFlowMap.get(v);

            if (blockFlow == null) {
                System.out.println(v + " is null");
                continue;
            }

            if (v.getType() == Block.BlockType.CONDITIONAL_BLOCK) {
                ConditionalBlock cv = (ConditionalBlock) v;
                System.out.println("Nodes" + cv.getNodes());
            }

            for (Block successor : v.getSuccessors()) {
                System.out.println("Succ " + v + " is " + successor);

                if (successor.getType().equals(Block.BlockType.CONDITIONAL_BLOCK)) {
                    ConditionalBlock conditionalSuccessor = (ConditionalBlock) successor;
                    sbDotNodes.append(makePdgEdge(blockFlow.outNode, statementFlowMap.get(conditionalSuccessor.getThenSuccessor()).inNode, EdgeType.CONTROL));
                    sbDotNodes.append(System.lineSeparator());
                    sbDotNodes.append(makePdgEdge(blockFlow.outNode, statementFlowMap.get(conditionalSuccessor.getElseSuccessor()).inNode, EdgeType.CONTROL));
                    sbDotNodes.append(System.lineSeparator());
                } else {
                    sbDotNodes.append(makePdgEdge(blockFlow.outNode, statementFlowMap.get(successor).inNode, EdgeType.CONTROL));
                    sbDotNodes.append(System.lineSeparator());
                }
            }
        }

        sbDotNodes.append(System.lineSeparator());
        for (Edge edge : cfgEdges) {
            sbDotNodes.append(makePdgEdge(edge.from, edge.to, EdgeType.CONTROL));
            sbDotNodes.append(System.lineSeparator());
        }

        // Visualize dataflow
        FlexemeDataflowStore dataflowStore = analysis.getResult().getStoreAfter(cfg.getRegularExitBlock());
        Set<org.checkerframework.checker.codechanges.Edge> edges = dataflowStore.getEdges();

        for (org.checkerframework.checker.codechanges.Edge edge : edges) {
            Node from = edge.getFrom().reference;

            long fromUuid;
            if (from.getBlock() == null) {
                fromUuid = cfg.getEntryBlock().getUid();
            } else {
                fromUuid = edge.getFrom().reference.getUid();
            }
            sbDotNodes.append(makePdgEdge("n" + fromUuid, "n" + edge.getTo().reference.getUid(), EdgeType.DATA));
        }

        return sbDotNodes.toString();
//        return super.visualizeNodes(blocks, cfg, analysis);
    }

    private String makePdgEdge(@NonNull final String from, @NonNull final String to,@NonNull final EdgeType type) {
        final StringBuilder sb = new StringBuilder(from + " -> " + to + " ");

        sb.append("[");
        sb.append("key=").append(type.key);
        sb.append(", style=").append(type.style);
        if(type.color != "black") {
            sb.append(", color=").append(type.color);
        }
        sb.append("];");
        return sb.toString();
    }

    @Override
    public String visualizeBlockNode(Node t, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        Tree tree = t.getTree();

        if (tree == null) {
            return "";
        }

        Element e = TreeUtils.elementFromTree(tree);
        JCTree jct = ((JCTree) t.getTree());

        // No if or while constructs bubble here.

        long lineStart = lineMap.getLineNumber(jct.getStartPosition());
        long lineEnd = lineMap.getLineNumber(jct.getPreferredPosition());

        System.out.println(t.getTree().toString() + " -> " + t.getTree().getKind() + " (" + t.getClass() + ") (" + jct.getClass() + ") " + t.getInSource() + " [" + lineStart + "-" + lineEnd + "]");

        // If there is no Block in the map, add it starting at this node.
        statementFlowMap.putIfAbsent(t.getBlock(), new BlockFlow("n" + t.getUid()));

        // Every node is potentially the last "real" node.
        BlockFlow blockFlow = statementFlowMap.get(t.getBlock());
        blockFlow.setOutNode("n" + t.getUid());

//        System.out.println("Keep");

        addStatementEdge("n" + t.getUid());

        return lineSeparator + formatNode(String.valueOf(t.getUid()), t.getTree().toString(), lineStart, lineEnd);
    }

    private String formatNode(String uid, String label, long lineStart, long lineEnd) {
        return "n" + uid + " [cluster=\"" + cluster + "\", label=\"" + label + "\", span=\"" + lineStart + "-" + lineEnd + "\"];";
    }

    private void addStatementEdge(String to) {
        if(lastStatementInBlock != null) {
            cfgEdges.add(new Edge(lastStatementInBlock, to));
        }
        lastStatementInBlock = to;
    }

    @Override
    public String getSeparator() {
        return "";
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return formatNode(String.valueOf(sbb.getUid()), sbb.getSpecialType().name(), 0,0);
    }

    @Override
    protected void handleSuccessorsHelper(Block cur, Set<Block> visited, Queue<Block> workList, StringBuilder sbGraph) {
        super.handleSuccessorsHelper(cur, visited, workList, sbGraph);
    }

    @Override
    protected String visualizeEdge(Object sId, Object eId, String flowRule) {
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
