package org.checkerframework.checker.codechanges;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.*;

public class FlexemeDataflowStore implements Store<FlexemeDataflowStore> {
    private final Map<String, FlexemeDataflowValue> lastUse;
    private final Set<Edge> edges;

    /** Create a new LiveVarStore. */
    public FlexemeDataflowStore(List<LocalVariableNode> parameters) {
        lastUse = new HashMap<>();
        edges = new LinkedHashSet<>();

        parameters.forEach(this::addParameter);
    }

    /**
     * Create a new FlexemeDataflowStore.
     */
    public FlexemeDataflowStore(Map<String, FlexemeDataflowValue> lastUse, Set<Edge> edges) {
        this.lastUse = lastUse;
        this.edges = edges;
    }

    public void addParameter(LocalVariableNode node) {
        if (lastUse.containsKey(node.getName())) {
            System.out.println("WARNING -- Declaration already present: " + node.getName());
            return;
        }

        lastUse.put(node.getName(), new FlexemeDataflowValue(node));
    }

    public void addLocalVariableDeclaration(VariableDeclarationNode node) {
        if (lastUse.containsKey(node.getName())) {
            System.out.println("WARNING -- Declaration already present: " + node.getName());
            return;
        }

        lastUse.put(node.getName(), new FlexemeDataflowValue(node));
    }

    /**
     * Add a new dataflow edge between the last and current n.
     * @param n
     */
    public void addDataflowEdge(LocalVariableNode n) {
        FlexemeDataflowValue last = this.lastUse.get(n.getName());

        FlexemeDataflowValue value = new FlexemeDataflowValue(n);
        edges.add(new Edge(last, value));

        this.lastUse.put(n.getName(), value);
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof FlexemeDataflowStore)) {
            return false;
        }
        FlexemeDataflowStore other = (FlexemeDataflowStore) obj;
        return other.lastUse.equals(this.lastUse) && other.edges.equals(this.edges);
    }

    @Override
    public int hashCode() {
        return this.lastUse.hashCode() + this.edges.hashCode();
    }

    @Override
    public FlexemeDataflowStore copy() {
        return new FlexemeDataflowStore(new HashMap<>(lastUse), new HashSet<>(edges));
    }

    @Override
    public FlexemeDataflowStore leastUpperBound(FlexemeDataflowStore other) {
        final Map<String, FlexemeDataflowValue> lastUseLub =
                new HashMap<>(this.lastUse.size() + other.lastUse.size());
        lastUseLub.putAll(this.lastUse);
        lastUseLub.putAll(other.lastUse);

        Set<Edge> edgesLub =
                new HashSet<>(this.edges.size() + other.edges.size());
        edgesLub.addAll(this.edges);
        edgesLub.addAll(other.edges);
        return new FlexemeDataflowStore(lastUseLub, edgesLub);
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

    private String visualizeLastUseStore(CFGVisualizer<?, FlexemeDataflowStore, ?> viz) {
        String key = "variables";
        if (lastUse.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (Map.Entry<String, FlexemeDataflowValue> entry : lastUse.entrySet()) {
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
}
