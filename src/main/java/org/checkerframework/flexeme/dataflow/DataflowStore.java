package org.checkerframework.flexeme.dataflow;

import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.flexeme.Util;
import org.checkerframework.javacutil.BugInCF;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A store that keeps track of
 * 1) when was a variable last used (i.e., declared or referred);
 * 2) the use edges between variable references.
 */
public class DataflowStore implements Store<DataflowStore> {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DataflowStore.class);
    private final Map<String, Set<DataflowValue>> lastUse;
    private final Set<Edge> edges;
    private final List<LocalVariableNode> parameters; // Initial variable declaration for method parameters.

    /**
     * Create a new FlexemeDataflowStore.
     */
    public DataflowStore(Map<String, Set<DataflowValue>> lastUse, Set<Edge> edges, List<LocalVariableNode> parameters) {
        this.lastUse = lastUse;
        this.edges = edges;
        this.parameters = parameters;
    }

    /**
     * Create a new FlexemeDataflowStore.
     * @param parameters The parameters of the method, added at the start since they are not declared while visiting the body of the method.
     */
    public DataflowStore(List<LocalVariableNode> parameters) {
        lastUse = new HashMap<>();
        edges = new LinkedHashSet<>();
        this.parameters = parameters;
        parameters.forEach(this::addParameter);
    }

    /**
     * Add a parameter to the store to keep track of.
     * @param node The parameter.
     */
    private void addParameter(LocalVariableNode node) {
        if (lastUse.containsKey(node.getName())) {
            return;
        }
        lastUse.put(node.getName(), Util.newSet(new DataflowValue(node)));
    }

    /**
     * Add a variable declaration to the store to keep track of.
     * @param node the variable declaration
     */
    public void addLocalVariableDeclaration(VariableDeclarationNode node) {
        if (lastUse.containsKey(node.getName())) {
            // Nothing to do, we already know of this variable.
            return;
        }
        // We mark that we encountered this variable for the first time.
        lastUse.put(node.getName(), Util.newSet(new DataflowValue(node)));
    }

    /**
     * Add a new dataflow edge between the last and current n.
     * @param n the variable reference
     */
    public void addDataflowEdge(LocalVariableNode n) {
        DataflowValue value = new DataflowValue(n);

        // Add a new edge between the last time this variable is used to this current reference.
        final Set<DataflowValue> lastUses = this.lastUse.get(n.getName());
        if (lastUses == null) {
            return;
        }

        for (DataflowValue last : this.lastUse.get(n.getName())) {
            edges.add(new Edge(last, value));
        }
        // The last use is this reference now.
        this.lastUse.put(n.getName(), Util.newSet(value));
    }

    @Override
    public DataflowStore copy() {
        return new DataflowStore(new HashMap<>(lastUse), new HashSet<>(edges), parameters);
    }

    @Override
    public DataflowStore leastUpperBound(DataflowStore other) {
        final Map<String, Set<DataflowValue>> lastUseLub = Util.mergeSetMaps(this.lastUse, other.lastUse);

        final Set<Edge> edgesLub =
                new HashSet<>(this.edges.size() + other.edges.size());
        edgesLub.addAll(this.edges);
        edgesLub.addAll(other.edges);
        return new DataflowStore(lastUseLub, edgesLub, parameters);
    }

    private String visualizeLastUseStore(CFGVisualizer<?, DataflowStore, ?> viz) {
        String key = "variables";
        if (lastUse.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (Map.Entry<String, Set<DataflowValue>> entry : lastUse.entrySet()) {
            sjStoreVal.add(entry.getKey());
        }

        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    private CharSequence visualizeEdges(CFGVisualizer<?, DataflowStore, ?> viz) {
        StringJoiner sjStoreVal = new StringJoiner(System.lineSeparator());
        for (Edge edge : edges) {
            sjStoreVal.add(edge.toString());
        }

        return viz.visualizeStoreKeyVal("Edges", sjStoreVal.toString());
    }

    public Set<Edge> getEdges() {
        return this.edges;
    }

    public List<LocalVariableNode> getParameters() {
        return this.parameters;
    }

    /** It should not be called since it is not used by the backward analysis. */
    @Override
    public DataflowStore widenedUpperBound(DataflowStore previous) {
        throw new BugInCF("wub of FlexemeDataflowStore get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, DataflowStore, ?> viz) {
        StringJoiner stores = new StringJoiner("\n");
        stores.add(visualizeLastUseStore(viz));
        stores.add(visualizeEdges(viz));
        return stores.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Variables: ");
        sb.append(lastUse.keySet());
        sb.append(System.lineSeparator());

        sb.append("Edges: ");
        sb.append(edges.stream().map(edge -> edge.getFrom().getReference().getUid() + " -> " + edge.getTo().getReference().getUid()).collect(Collectors.joining(",")));
        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataflowStore that = (DataflowStore) o;
        return lastUse.equals(that.lastUse) && edges.equals(that.edges) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastUse, edges, parameters);
    }
}
