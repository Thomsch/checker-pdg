package org.checkerframework.flexeme.pdg;

/**
 * Represent a node in a PDG.
 * A PDG node is a PDG element (See {@link org.checkerframework.flexeme.PdgElementScanner}) or a special entry/exit node from the CFG such as
 * 'Entry', 'Exit', or 'ExceptionalExit'.
 */
public class PdgNode {
    private final MethodPdg pdg;
    private final String label;
    private final long id;
    private final long startLine;
    private final long endLine;

    public PdgNode(MethodPdg pdg, final long nodeId, final String label, final long lineStart, final long lineEnd) {
        this.pdg = pdg;
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

    public MethodPdg getPdg() {
        return pdg;
    }
}
