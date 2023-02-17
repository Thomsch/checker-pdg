package org.checkerframework.flexeme.dataflow;

import java.util.Objects;

public class Edge {
    private final DataflowValue from;
    private final DataflowValue to;

    public Edge(DataflowValue from, DataflowValue to) {
        this.from = from;
        this.to = to;
    }

    public DataflowValue getFrom() {
        return from;
    }

    public DataflowValue getTo() {
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
