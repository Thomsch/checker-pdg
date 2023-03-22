package org.checkerframework.flexeme.dataflow;

import java.util.Objects;

/**
 * Represents a dataflow edge in the dataflow graph.
 */
public class Edge {
    private final VariableReference from;
    private final VariableReference to;

    public Edge(VariableReference from, VariableReference to) {
        this.from = from;
        this.to = to;
    }

    public VariableReference getFrom() {
        return from;
    }

    public VariableReference getTo() {
        return to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return from.toString() + " -> " + to.toString();
    }
}
