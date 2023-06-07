package org.checkerframework.flexeme;

import com.sun.source.tree.Tree;

public class PdgNode {
    private long id;
    private final String label;
    private long startLine;
    private long endLine;

    public PdgNode(final long nodeId, final String label, final long lineStart, final long lineEnd) {
        this.id = nodeId;
        this.label = label;
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
        return label;
    }
}
