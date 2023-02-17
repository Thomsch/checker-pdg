package org.checkerframework.flexeme.nameflow;

import org.checkerframework.com.google.common.collect.Sets;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.*;

import static org.checkerframework.flexeme.Util.mergeHashMaps;

/**
 * Represents the store of the NameFlow analysis.
 */
public class NameFlowStore implements Store<NameFlowStore> {

    // Map Xi of <variable node, set of names: {(snd,v),(40,l)}
    private final Map<String, Set<Name>> xi ;

    public NameFlowStore() {
        this.xi = new HashMap<>();
    }

    public NameFlowStore(final Map<String, Set<Name>> xi) {
        this.xi = xi;
    }

    public void add(final String variable, final Name name) {
        xi.computeIfAbsent(variable, k -> new HashSet<>());
        xi.get(variable).add(name);
    }

    @Override
    public NameFlowStore copy() {
        return new NameFlowStore(new HashMap<>(xi));
    }

    @Override
    public NameFlowStore leastUpperBound(final NameFlowStore other) {
        final Map<String, Set<Name>> xiLub = mergeHashMaps(this.xi, other.xi, Sets::union);
        return new NameFlowStore(xiLub);
    }

    @Override
    public NameFlowStore widenedUpperBound(final NameFlowStore nameFlowStore) {
        throw new BugInCF("wub of NameFlowStore get called!");
    }

    @Override
    public boolean canAlias(final JavaExpression javaExpression, final JavaExpression javaExpression1) {
        return true;
    }

    @Override
    public String visualize(final CFGVisualizer<?, NameFlowStore, ?> cfgVisualizer) {
        throw new RuntimeException("Visualization not supported for NameFlowStore.");
    }

    @Override
    public int hashCode() {
        return Objects.hash(xi);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameFlowStore that = (NameFlowStore) o;
        return xi.equals(that.xi);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Ξ = {");
        sb.append(System.lineSeparator());
        xi.forEach((variable, names) -> {
            sb.append("  ");
            sb.append(variable);
            sb.append(": ");
            sb.append(names);
            sb.append(System.lineSeparator());
        });
        sb.append("}");

        return sb.toString();
    }
}
