package org.checkerframework.flexeme.pdg;

import com.google.common.graph.*;
import com.sun.source.tree.*;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
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

    private final MutableNetwork<PdgNode, PdgEdge> graph;

    private static long nodeId = 0; // TODO: Refactor nodeId to be a field of PdgNode
    private final HashMap<SpecialBlock, PdgNode> blockToPdgNode;
    private final Map<Node, Tree> cfgNodeToPdgTree; // Holds the mapping from CFG nodes to PDG nodes. One PDG nodes can be mapped to multiple CFG nodes.
    private final HashMap<Tree, PdgNode> pdgElementToPdgNodeMap;

    public final Set<String> parameterNames;

    public MethodPdg(FileProcessor processor, final ClassTree classAst, final MethodTree methodAst, final ControlFlowGraph methodCfg, final Map<Node, Tree> cfgNodesToPdgElements) {
        this.processor = processor;
        this.classAst = classAst;
        this.methodAst = methodAst;
        this.methodCfg = methodCfg;
        this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
        this.pdgElementToPdgNodeMap = new HashMap<>();
        this.blockToPdgNode = new HashMap<>();
        this.cfgNodeToPdgTree = cfgNodesToPdgElements;
        this.parameterNames = new HashSet<>();

        for (final VariableTree parameter : methodAst.getParameters()) {
            parameterNames.add(parameter.getName().toString());
        }
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

    public Set<PdgEdge> edges() {
        return graph.edges();
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
        graph.addNode(node);
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
        graph.addEdge(edge.from, edge.to, edge);
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

    public MethodTree getMethodAst() {
        return methodAst;
    }

    public ControlFlowGraph getMethodCfg() {
        return methodCfg;
    }

    /**
     * Returns a list of types of the parameters of the method.
     * @return a list of types of the parameters of the method
     */
    public List<String> getParametersType() {
        final List<String> parametersType = new ArrayList<>();
        for (final VariableTree parameter : methodAst.getParameters()) {
            parametersType.add(parameter.getType().toString());
        }
        return parametersType;
    }
}
