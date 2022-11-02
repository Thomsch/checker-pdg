package org.checkerframework.checker.codechanges;

import com.sun.source.tree.LineMap;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.Element;
import java.util.*;

public class FlexemePDGVisualizer extends DOTCFGVisualizer<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> {
    private final String cluster;
    private final LineMap lineMap;

    private String lastStatementInBlock;
    private List<Edge> cfgEdges;

    public FlexemePDGVisualizer(String cluster, LineMap lineMap) {
        super();
        this.cluster = cluster;
        this.lineMap = lineMap;
        cfgEdges = new ArrayList<>();
        lastStatementInBlock = null;
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

        sbDotNodes.append(System.lineSeparator());
        for (Edge edge : cfgEdges) {
            sbDotNodes.append(edge.from + " -> " + edge.to + System.lineSeparator());
        }

        return sbDotNodes.toString();
//        return super.visualizeNodes(blocks, cfg, analysis);
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

        if (!t.getClass().getSimpleName().equals("AssignmentNode")) {
            return "";
        }

        System.out.println("Keep");

        addStatementEdge("n" + t.getUid());


        //        cluster="CommandLine.Infrastructure.EnumerableExtensions.IndexOf<TSource>(System.Collections.Generic.IEnumerable<TSource>, System.Func<TSource, bool>)", label=Entry, span="10-10"
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
