package org.checkerframework.flexeme.nameflow;

import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
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
    private final Map<String, Node> variables;
    private final Map<String, Node> returnedVariables;

    public NameFlowStore(final List<LocalVariableNode> parameters) {
        this.xi = new HashMap<>();
        this.names = new HashMap<>();
        this.variables = new HashMap<>();
        parameters.forEach(this::addParameter);
        returnedVariables = new HashMap<>();
    }

    private void addParameter(final LocalVariableNode localVariableNode) {
        this.variables.put(localVariableNode.getName(), localVariableNode);
    }

    public NameFlowStore(final Map<Node, Set<NameRecord>> xi, final Map<String, String> names, final Map<String, Node> variables, final Map<String, Node> returnedVariables) {
        this.xi = xi;
        this.names = names;
        this.variables = variables;
        this.returnedVariables = returnedVariables;
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
        return new NameFlowStore(new HashMap<>(xi), new HashMap<>(names), new HashMap<>(variables), new HashMap<>(returnedVariables));
    }

    @Override
    public NameFlowStore leastUpperBound(final NameFlowStore other) {
        // The names of each store are saved. If the names are the same, the corresponding sets are merged.
        final Map<Node, Set<NameRecord>> xiLub = Util.mergeSetMaps(this.xi, other.xi);
        this.variables.putAll(other.variables);
        this.returnedVariables.putAll(other.returnedVariables);
        return new NameFlowStore(xiLub, this.names, this.variables, this.returnedVariables);
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

        sb.append(System.lineSeparator());
        variables.forEach((name, node) -> {
            sb.append(name);
            sb.append(": ");
            sb.append(node);
            sb.append(System.lineSeparator());
        });

        return sb.toString();
    }

    public void registerVariableDeclaration(final VariableDeclarationNode node) {
        variables.put(node.getName(), node);
    }

    public Node getVariableNode(final String name) {
        return variables.get(name);
    }

    public void addReturnedVariables(final ReturnNode n) {
        if (n.getResult() == null) {
            return;
        }
        final Collection<Node> nodes = n.getTransitiveOperands();
        nodes.stream()
                .filter(node -> node instanceof LocalVariableNode)
                .forEach(node -> returnedVariables.put(((LocalVariableNode) node).getName(), node));
    }

    public Map<String, Node> getReturnedVariables() {
        return returnedVariables;
    }
}
