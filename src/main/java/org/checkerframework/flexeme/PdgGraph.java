package org.checkerframework.flexeme;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;

import java.util.Collection;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class PdgGraph {
    private final FileProcessor processor;
    private final ClassTree classTree;
    private final MethodTree methodTree;

    private final  MutableNetwork<PdgNode, Object> graph;

    private static long nodeId = 0;

    PdgGraph(FileProcessor processor, final ClassTree classTree, final MethodTree methodTree) {
        this.processor = processor;
        this.classTree = classTree;
        this.methodTree = methodTree;
        this.graph = NetworkBuilder.directed().allowsSelfLoops(true).allowsParallelEdges(true).build();
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
        PdgNode node = new PdgNode(nodeId, tree, lineStart, lineEnd, tree);
        graph.addNode(node);
        nodeId++;
    }
}
