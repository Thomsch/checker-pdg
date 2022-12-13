package org.checkerframework.checker.codechanges;

import org.checkerframework.com.google.common.collect.Sets;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.*;
import java.util.function.BiFunction;

public class FlexemeDataflowStore implements Store<FlexemeDataflowStore> {
    private final Map<String, Set<FlexemeDataflowValue>> lastUse;
    private final Set<Edge> edges;
    private final List<LocalVariableNode> parameters;

    public FlexemeDataflowStore(List<LocalVariableNode> parameters) {
        lastUse = new HashMap<>();
        edges = new LinkedHashSet<>();
        this.parameters = parameters;
        parameters.forEach(this::addParameter);
    }

    /**
     * Create a new FlexemeDataflowStore.
     */
    public FlexemeDataflowStore(Map<String, Set<FlexemeDataflowValue>> lastUse, Set<Edge> edges, List<LocalVariableNode> parameters) {
        this.lastUse = lastUse;
        this.edges = edges;
        this.parameters = parameters;
    }

    public void addParameter(LocalVariableNode node) {
        if (lastUse.containsKey(node.getName())) {
            return;
        }

        lastUse.put(node.getName(), Sets.newHashSet(new FlexemeDataflowValue(node)));
    }

    public void addLocalVariableDeclaration(VariableDeclarationNode node) {
        if (lastUse.containsKey(node.getName())) {
            return;
        }
        lastUse.put(node.getName(), Sets.newHashSet(new FlexemeDataflowValue(node)));
    }

    /**
     * Add a new dataflow edge between the last and current n.
     * @param n
     */
    public void addDataflowEdge(LocalVariableNode n) {
        FlexemeDataflowValue value = new FlexemeDataflowValue(n);
        for (FlexemeDataflowValue last : this.lastUse.get(n.getName())) {
            edges.add(new Edge(last, value));
        }

        this.lastUse.put(n.getName(), Sets.newHashSet(value));
    }

    @Override
    public FlexemeDataflowStore copy() {
        return new FlexemeDataflowStore(new HashMap<>(lastUse), new HashSet<>(edges), parameters);
    }

    @Override
    public FlexemeDataflowStore leastUpperBound(FlexemeDataflowStore other) {
        final Map<String, Set<FlexemeDataflowValue>> lastUseLub = mergeHashMaps(this.lastUse, other.lastUse, Sets::union);
        final Set<Edge> edgesLub =
                new HashSet<>(this.edges.size() + other.edges.size());
        edgesLub.addAll(this.edges);
        edgesLub.addAll(other.edges);
        return new FlexemeDataflowStore(lastUseLub, edgesLub, parameters);
    }

    /**
     * Merge two Maps.
     * @param a The first Map
     * @param b The second Map
     * @param mergeValues Function that defines how to merge two of the values together.
     * @return A new Map instance containing the keys and values of a and b maps.
     * @param <K> Generic type for map keys
     * @param <V> Generic type for map values
     */
    private <K, V> Map<K, V> mergeHashMaps(Map<K, V> a, Map<K, V> b, BiFunction<V, V, V> mergeValues) {
        Map<K, V> result = new HashMap<>(a.size() + b.size());

        // Optimize merge performance loss in big hashmaps (e.g., after a long branch).
        Map<K, V> big;
        Map<K, V> small;
        if (a.size() > b.size()) {
            big = a;
            small = b;
        } else {
            big = b;
            small = a;
        }

        result.putAll(big);
        small.forEach((key, value) -> result.merge(key, value, mergeValues));
        return result;
    }

    private String visualizeLastUseStore(CFGVisualizer<?, FlexemeDataflowStore, ?> viz) {
        String key = "variables";
        if (lastUse.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (Map.Entry<String, Set<FlexemeDataflowValue>> entry : lastUse.entrySet()) {
            sjStoreVal.add(entry.getKey());
        }

        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    private CharSequence visualizeEdges(CFGVisualizer<?, FlexemeDataflowStore, ?> viz) {
        StringJoiner sjStoreVal = new StringJoiner("\n");
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
    public FlexemeDataflowStore widenedUpperBound(FlexemeDataflowStore previous) {
        throw new BugInCF("wub of LiveVarStore get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, FlexemeDataflowStore, ?> viz) {
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
        sb.append("\n");

        sb.append("Edges: ");
        sb.append(edges.toString());
        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlexemeDataflowStore that = (FlexemeDataflowStore) o;
        return lastUse.equals(that.lastUse) && edges.equals(that.edges) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastUse, edges, parameters);
    }

    //    /**
    //     * Add the information of live variables in an expression to the live variable set.
    //     *
    //     * @param expression a node
    //     */
    //    public void addUseInExpression(Node expression) {
    //        // TODO Do we need a AbstractNodeScanner to do the following job?
    //        if (expression instanceof LocalVariableNode || expression instanceof FieldAccessNode) {
    //            FlexemeDataflowValue liveVarValue = new FlexemeDataflowValue(expression);
    //            putLiveVar(liveVarValue);
    //        } else if (expression instanceof UnaryOperationNode) {
    //            UnaryOperationNode unaryNode = (UnaryOperationNode) expression;
    //            addUseInExpression(unaryNode.getOperand());
    //        } else if (expression instanceof TernaryExpressionNode) {
    //            TernaryExpressionNode ternaryNode = (TernaryExpressionNode) expression;
    //            addUseInExpression(ternaryNode.getConditionOperand());
    //            addUseInExpression(ternaryNode.getThenOperand());
    //            addUseInExpression(ternaryNode.getElseOperand());
    //        } else if (expression instanceof TypeCastNode) {
    //            TypeCastNode typeCastNode = (TypeCastNode) expression;
    //            addUseInExpression(typeCastNode.getOperand());
    //        } else if (expression instanceof InstanceOfNode) {
    //            InstanceOfNode instanceOfNode = (InstanceOfNode) expression;
    //            addUseInExpression(instanceOfNode.getOperand());
    //        } else if (expression instanceof BinaryOperationNode) {
    //            BinaryOperationNode binaryNode = (BinaryOperationNode) expression;
    //            addUseInExpression(binaryNode.getLeftOperand());
    //            addUseInExpression(binaryNode.getRightOperand());
    //        }
    //    }
}
