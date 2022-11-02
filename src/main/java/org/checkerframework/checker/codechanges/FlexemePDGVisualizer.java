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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class FlexemePDGVisualizer extends DOTCFGVisualizer<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> {
    private final String cluster;
    private final LineMap lineMap;

    public FlexemePDGVisualizer(String cluster, LineMap lineMap) {
        super();
        this.cluster = cluster;
        this.lineMap = lineMap;
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
        }
        return sbDotNodes.toString();
//        return super.visualizeNodes(blocks, cfg, analysis);
    }

    @Override
    public String visualizeBlockNode(Node t, @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        Tree tree = t.getTree();
        Element e = TreeUtils.elementFromTree(tree);

//        TODO: Ignore temporary variables by looking at their name.
//        TODO: Remove duplicate nodes for same code statement.

        JCTree jct = ((JCTree) t.getTree());
        long lineStart = lineMap.getLineNumber(jct.getStartPosition());
        long lineEnd = lineMap.getLineNumber(jct.getPreferredPosition());

        System.out.println(t.getTree().toString() + " -> " + t.getTree().getKind() + " (" + t.getClass() + ") " + t.getInSource() + " [" + lineStart + "-" + lineEnd + "]");

        //        cluster="CommandLine.Infrastructure.EnumerableExtensions.IndexOf<TSource>(System.Collections.Generic.IEnumerable<TSource>, System.Func<TSource, bool>)", label=Entry, span="10-10"
        return lineSeparator + "n" + t.getUid() + " [cluster=\"" + cluster + "\", label=\"" + t.getTree().toString() + "\", span=\"" + lineStart + "-" + lineEnd + "\"];";
    }

    @Override
    public String getSeparator() {
        return "";
    }

    @Override
    public String visualizeSpecialBlock(SpecialBlock sbb) {
        return "";
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
