package org.checkerframework.flexeme.pdg;

import com.google.common.graph.*;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.flexeme.FileProcessor;

import java.util.*;

/**
 * Represent the Program Dependence Graph (PDG) of a method.
 */
@SuppressWarnings("UnstableApiUsage")
public class MethodPdg {
    private final FileProcessor processor;
    private final ClassTree classAst;
    private final MethodTree methodAst;
    private final ControlFlowGraph methodCfg;

    private final MutableValueGraph<PdgNode, PdgEdge.Type> graph;

    private static long nodeId = 0; // TODO: Refactor nodeId to be a field of PdgNode
    private final HashMap<SpecialBlock, PdgNode> blockToPdgNode;
    private final Map<Node, Tree> cfgNodeToPdgTree; // Holds the mapping from CFG nodes to PDG nodes. One PDG nodes can be mapped to multiple CFG nodes.
    private HashMap<Tree, PdgNode> pdgElementToPdgNodeMap;

    public MethodPdg(FileProcessor processor, final ClassTree classAst, final MethodTree methodAst, final ControlFlowGraph methodCfg, final Map<Node, Tree> cfgNodesToPdgElements) {
        this.processor = processor;
        this.classAst = classAst;
        this.methodAst = methodAst;
        this.methodCfg = methodCfg;
        this.graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        // this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
        this.pdgElementToPdgNodeMap = new HashMap<>();
        this.blockToPdgNode = new HashMap<>();
        this.cfgNodeToPdgTree = cfgNodesToPdgElements;
    }

    public String getClassName() {
        return classAst.getSimpleName().toString();
    }

    public String getMethodName() {
        return methodAst.getName().toString();
    }

    public Set<PdgNode> nodes() {
        return graph.nodes();
    }

    public void addNode(final Tree tree) {
        final LineMap lineMap = processor.getLineMap();
        final EndPosTable endPosTable = processor.getEndPosTable();
        JCTree jct = (JCTree) tree;
        long lineStart = lineMap.getLineNumber(jct.getStartPosition());
        long lineEnd = lineMap.getLineNumber(jct.getEndPosition(endPosTable));
        PdgNode node = new PdgNode(this, nodeId, tree.toString(), lineStart, lineEnd);
        pdgElementToPdgNodeMap.put(tree, node);
        graph.addNode(node);
        nodeId++;
    }

    public Set<EndpointPair<PdgNode>> edges() {
        return graph.edges();
    }

    public Optional<PdgEdge.Type> edgeValue(final EndpointPair<PdgNode> edge) {
        return graph.edgeValue(edge);
    }

    /**
     * Registers a special block in the PDG and pre-creates a PDG node with the correct label.
     * The PDG node will be ready to use when associated with an PDG edge.
     * @param block the special block to register
     * @param label the label to use for the PDG node
     */
    public void registerSpecialBlock(final SpecialBlock block, final String label) {
        final PdgNode node = new PdgNode(this, nodeId, label, 0, 0);
        blockToPdgNode.put(block, node);
        nodeId++;
    }


    public PdgNode getNode(final SpecialBlock block) {
        return blockToPdgNode.get(block);
    }

    public PdgNode getNode(final Node node) {
        final Tree tree = cfgNodeToPdgTree.get(node);
        return pdgElementToPdgNodeMap.get(tree);
    }

    public PdgNode getNode(final Tree tree) {
        return pdgElementToPdgNodeMap.get(tree);
    }

    public void addEdge(final PdgEdge edge) {
        graph.putEdgeValue(edge.from, edge.to, edge.type);
    }

    /**
     * Checks if the graph contains a node with the given label.
     * @param nodeLabel the label to find
     * @return true if the graph contains a node with the given label, false otherwise
     */
    public boolean containsNode(final String nodeLabel) {
        for (PdgNode node : graph.nodes()) {
            if (node.toString().equals(nodeLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the graph contains an edge with the given labels.
     * @param fromLabel the label of the source node
     * @param toLabel the label of the target node
     * @return true if the graph contains an edge with the given labels, false otherwise
     */
    public boolean containsEdge(final String fromLabel, final String toLabel) {
        for (EndpointPair<PdgNode> edge : graph.edges()) {
            if (edge.nodeU().toString().equals(fromLabel) && edge.nodeV().toString().equals(toLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the AST method tree associated with this PDG.
     * @return the AST method tree
     */
    public MethodTree getTree() {
        return methodAst;
    }

    /**
     * Returns the {@link PdgNode} that represents the entry point of the PDG.
     * @return the entry point of the PDG
     */
    public PdgNode getStartNode() {
        final SpecialBlock entryBlock = methodCfg.getEntryBlock();
        return getNode(entryBlock);
    }

    public Set<Tree> getPdgElements() {
        return pdgElementToPdgNodeMap.keySet();
    }
}
