package org.checkerframework.checker.codechanges;

import java.util.Objects;

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
