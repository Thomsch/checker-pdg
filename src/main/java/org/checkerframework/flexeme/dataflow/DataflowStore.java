package org.checkerframework.flexeme.dataflow;

import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
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

    // Map from variable name to the last time it was used.
    // The keys are the variable name e.g., 'foo' and the values are pointing to the last instance of that variable in the CFG.
    private final Map<String, Set<VariableReference>> lastUse;
    private final Set<Edge> edges;
    private final List<LocalVariableNode> parameters; // Initial variable declaration for method parameters.

    /**
     * Create a new DataflowStore.
     */
    public DataflowStore(Map<String, Set<VariableReference>> lastUse, Set<Edge> edges, List<LocalVariableNode> parameters) {
        this.lastUse = lastUse;
        this.edges = edges;
        this.parameters = parameters;
    }

    /**
     * Create a new DataflowStore.
     *
     * @param parameters The parameters of the method, added at the start since they are not declared while visiting the body of the method.
     */
    public DataflowStore(List<LocalVariableNode> parameters) {
        lastUse = new HashMap<>();
        edges = new LinkedHashSet<>();
        this.parameters = parameters;
        parameters.forEach(this::addParameter);
    }

    @Override
    public DataflowStore copy() {
        return new DataflowStore(new HashMap<>(lastUse), new HashSet<>(edges), parameters);
    }

    /**
     * Add a parameter to the store to keep track of.
     *
     * @param node The parameter.
     */
    private void addParameter(LocalVariableNode node) {
        if (lastUse.containsKey(node.getName())) {
            // We can't have two parameters with the same name.
            throw new RuntimeException("Parameter " + node.getName() + " is declared more than once.");
        }
        lastUse.put(node.getName(), Util.newSet(new VariableReference(node)));
    }

    /**
     * Register a new assignment to a variable. The variable can be already discovered or not.
     *
     * @param node the assignment node
     */
    public void registerAssignment(final AssignmentNode node) {
        if (node.getTarget() instanceof LocalVariableNode) {
            // If the target is a variable already declared, we need to add an edge to it.
            addDataflowEdge((LocalVariableNode) node.getTarget());
        }
        lastUse.put(node.getTarget().toString(), Util.newSet(new VariableReference(node.getTarget())));
    }

    /**
     * Add a new dataflow edge between the last and current n.
     *
     * @param n the variable reference
     */
    public void addDataflowEdge(LocalVariableNode n) {
        VariableReference value = new VariableReference(n);

        // Add a new edge between the last time this variable is used to this current reference.
        final Set<VariableReference> lastUses = this.lastUse.get(n.getName());
        if (lastUses == null) {
            return;
        }

        for (VariableReference last : this.lastUse.get(n.getName())) {
            edges.add(new Edge(last, value));
        }
        // The last use is this reference now.
        this.lastUse.put(n.getName(), Util.newSet(value));
    }


    @Override
    public DataflowStore leastUpperBound(DataflowStore other) {
        final Map<String, Set<VariableReference>> lastUseLub = Util.mergeSetMaps(this.lastUse, other.lastUse);

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

        for (final String k : lastUse.keySet()) {
            sjStoreVal.add(k);
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
        StringJoiner stores = new StringJoiner(System.lineSeparator());
        stores.add(visualizeLastUseStore(viz));
        stores.add(visualizeEdges(viz));
        return stores.toString();
    }

    @Override
    public String toString() {

        final String sb = "Variables: " +
                lastUse.keySet() +
                System.lineSeparator() +
                "Edges: " +
                edges.stream().map(Edge::toString).collect(Collectors.joining(","));
        return sb;
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
