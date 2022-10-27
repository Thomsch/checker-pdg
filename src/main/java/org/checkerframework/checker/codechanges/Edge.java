package org.checkerframework.checker.codechanges;

import org.checkerframework.dataflow.cfg.node.LocalVariableNode;

public class Edge {
    private final FlexemeDataflowValue from;
    private final FlexemeDataflowValue to;

    public Edge(FlexemeDataflowValue from, FlexemeDataflowValue to) {
        this.from = from;
        this.to = to;
    }

    public FlexemeDataflowValue getFrom() {
        return from;
    }

    public FlexemeDataflowValue getTo() {
        return to;
    }

    @Override
    public String toString() {
        return from.toString() + " -> " + to.toString();
    }
}
