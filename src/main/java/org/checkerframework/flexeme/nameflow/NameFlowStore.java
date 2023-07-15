package org.checkerframework.flexeme.nameflow;

import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.flexeme.Util;
import org.checkerframework.javacutil.BugInCF;

import java.util.*;

/**
 * Represents the store of the NameFlow analysis.
 */
public class NameFlowStore implements Store<NameFlowStore> {

    // Store the edges between nodes representing the nameflow.
    public final Map<String, String> names;

    // Store the return value of the Ξ function in "RefiNym: Using Names to Refine Types".
    // The key is a variable node, the values is set of names: {(snd,v),(40,l)}
    private final Map<Node, Set<NameRecord>> xi;

    public NameFlowStore() {
        this.xi = new HashMap<>();
        this.names = new HashMap<>();
    }

    public NameFlowStore(final Map<Node, Set<NameRecord>> xi, final Map<String, String> names) {
        this.xi = xi;
        this.names = names;
    }

    /**
     * Associate a potentially new nameRecord to a variable.
     * @param uid The node id
     * @param targetName The target id
     * @param nameRecord The new nameRecord to associate
     */
    public void add(final Node uid, final String targetName, final NameRecord nameRecord) {
        xi.computeIfAbsent(uid, k -> new HashSet<>());
        xi.get(uid).add(nameRecord);
        names.put(Long.toString(uid.getUid()), targetName);
    }

    public Map<Node, Set<NameRecord>> getXi() {
        return xi;
    }

    @Override
    public NameFlowStore copy() {
        return new NameFlowStore(new HashMap<>(xi), new HashMap<>(names));
    }

    @Override
    public NameFlowStore leastUpperBound(final NameFlowStore other) {
        // The names of each store are saved. If the names are the same, the corresponding sets are merged.
        final Map<Node, Set<NameRecord>> xiLub = Util.mergeSetMaps(this.xi, other.xi);
        return new NameFlowStore(xiLub, this.names);
    }

    @Override
    public NameFlowStore widenedUpperBound(final NameFlowStore nameFlowStore) {
        throw new BugInCF("wub of NameFlowStore get called!");
    }

    @Override
    public boolean canAlias(final JavaExpression javaExpression1, final JavaExpression javaExpression2) {
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
        xi.forEach((variable, nameSet) -> {
            sb.append("  ");
            sb.append(variable);
            sb.append(": ");
            sb.append(nameSet);
            sb.append(System.lineSeparator());
        });
        sb.append("}");

        return sb.toString();
    }
}
