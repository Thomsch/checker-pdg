package org.checkerframework.flexeme;

import com.google.common.graph.*;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class PdgGraph {
    private final FileProcessor processor;
    private final ClassTree classTree;
    private final MethodTree methodTree;

    private final MutableValueGraph<PdgNode, PdgEdge.Type> graph;

    private static long nodeId = 0; // TODO: Refactor nodeId to be a field of PdgNode
    private final HashMap<SpecialBlock, PdgNode> blockToPdgNode;
    private final Map<Node, Tree> cfgNodeToPdgTree; // Holds the mapping from CFG nodes to PDG nodes. One PDG nodes can be mapped to multiple CFG nodes.
    private HashMap<Tree, PdgNode> treeToNodeMap;

    PdgGraph(FileProcessor processor, final ClassTree classTree, final MethodTree methodTree) {
        this.processor = processor;
        this.classTree = classTree;
        this.methodTree = methodTree;
        this.graph = ValueGraphBuilder.directed().allowsSelfLoops(true).build();
        // this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
        this.treeToNodeMap = new HashMap<>();
        this.blockToPdgNode = new HashMap<>();
        this.cfgNodeToPdgTree = processor.getNodeMap().get(methodTree);
    }

    public String getClassName() {
        return classTree.getSimpleName().toString();
    }

    public String getMethodName() {
        return methodTree.getName().toString();
    }

    public Set<PdgNode> nodes() {
        return graph.nodes();
    }

    public void addNode(final Tree tree) {
        final LineMap lineMap = processor.getLineMap();
        JCTree jct = (JCTree) tree;
        long lineStart = lineMap.getLineNumber(jct.getStartPosition());
        long lineEnd = lineMap.getLineNumber(jct.getPreferredPosition());
        PdgNode node = new PdgNode(nodeId, tree.toString(), lineStart, lineEnd);
        treeToNodeMap.put(tree, node);
        graph.addNode(node);
        nodeId++;
    }

    public Set<EndpointPair<PdgNode>> edges() {
        return graph.edges();
    }

    public Optional<PdgEdge.Type> edgeValue(final EndpointPair<PdgNode> edge) {
        return graph.edgeValue(edge);
    }

    public void addEntryNode(final SpecialBlock entryBlock) {
        final PdgNode node = new PdgNode(nodeId, "Entry", 0, 0); // Flexeme requires to have "Entry" node (case sensitive)
        blockToPdgNode.put(entryBlock, node);
        nodeId++;
    }

    public void addExitNode(final SpecialBlock regularExitBlock) {
        final PdgNode node = new PdgNode(nodeId, "Exit", 0, 0); // Flexeme requires to have "Exit" node (case sensitive)
        blockToPdgNode.put(regularExitBlock, node);
        nodeId++;
    }

    public void addExceptionalExitNode(final SpecialBlock exceptionalExitBlock) {
        final PdgNode node = new PdgNode(nodeId, "ExceptionalExit", 0, 0);
        blockToPdgNode.put(exceptionalExitBlock, node);
        nodeId++;
    }

    public PdgNode getNode(final SpecialBlock block) {
        return blockToPdgNode.get(block);
    }

    public PdgNode getNode(final Node node) {
        final Tree tree = cfgNodeToPdgTree.get(node);
        return treeToNodeMap.get(tree);
    }

    public void addEdge(final PdgEdge edge) {
        graph.putEdgeValue(edge.from, edge.to, edge.type);
    }
}