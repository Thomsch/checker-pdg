package org.checkerframework.flexeme;

import com.sun.source.tree.Tree;

public class PdgNode {
    private long id;
    private final Tree tree;
    private long startLine;
    private long endLine;

    public PdgNode(final long nodeId, final Tree tree, final long lineStart, final long lineEnd, final Tree tree1) {
        this.id = nodeId;
        this.tree = tree;
        this.startLine = lineStart;
        this.endLine = lineEnd;
    }

    public long getId() {
        return id;
    }

    public long getStartLine() {
        return startLine;
    }

    public long getEndLine() {
        return endLine;
    }

    @Override
    public String toString() {
        return tree.toString();
    }
}
