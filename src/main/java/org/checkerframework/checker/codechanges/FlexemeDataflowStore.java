package org.checkerframework.checker.codechanges;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

public class FlexemeDataflowStore implements Store<FlexemeDataflowStore> {

    /** A set of live variable abstract values. */
    private final Set<FlexemeDataflowValue> liveVarValueSet;

    /** Create a new LiveVarStore. */
    public FlexemeDataflowStore() {
        liveVarValueSet = new LinkedHashSet<>();
    }

    /**
     * Create a new LiveVarStore.
     *
     * @param liveVarValueSet a set of live variable abstract values
     */
    public FlexemeDataflowStore(Set<FlexemeDataflowValue> liveVarValueSet) {
        this.liveVarValueSet = liveVarValueSet;
    }

    /**
     * Add the information of a live variable into the live variable set.
     *
     * @param variable a live variable
     */
    public void putLiveVar(FlexemeDataflowValue variable) {
        liveVarValueSet.add(variable);
    }

    /**
     * Remove the information of a live variable from the live variable set.
     *
     * @param variable a live variable
     */
    public void killLiveVar(FlexemeDataflowValue variable) {
        liveVarValueSet.remove(variable);
    }

    /**
     * Add the information of live variables in an expression to the live variable set.
     *
     * @param expression a node
     */
    public void addUseInExpression(Node expression) {
        // TODO Do we need a AbstractNodeScanner to do the following job?
        if (expression instanceof LocalVariableNode || expression instanceof FieldAccessNode) {
            FlexemeDataflowValue liveVarValue = new FlexemeDataflowValue(expression);
            putLiveVar(liveVarValue);
        } else if (expression instanceof UnaryOperationNode) {
            UnaryOperationNode unaryNode = (UnaryOperationNode) expression;
            addUseInExpression(unaryNode.getOperand());
        } else if (expression instanceof TernaryExpressionNode) {
            TernaryExpressionNode ternaryNode = (TernaryExpressionNode) expression;
            addUseInExpression(ternaryNode.getConditionOperand());
            addUseInExpression(ternaryNode.getThenOperand());
            addUseInExpression(ternaryNode.getElseOperand());
        } else if (expression instanceof TypeCastNode) {
            TypeCastNode typeCastNode = (TypeCastNode) expression;
            addUseInExpression(typeCastNode.getOperand());
        } else if (expression instanceof InstanceOfNode) {
            InstanceOfNode instanceOfNode = (InstanceOfNode) expression;
            addUseInExpression(instanceOfNode.getOperand());
        } else if (expression instanceof BinaryOperationNode) {
            BinaryOperationNode binaryNode = (BinaryOperationNode) expression;
            addUseInExpression(binaryNode.getLeftOperand());
            addUseInExpression(binaryNode.getRightOperand());
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof FlexemeDataflowStore)) {
            return false;
        }
        FlexemeDataflowStore other = (FlexemeDataflowStore) obj;
        return other.liveVarValueSet.equals(this.liveVarValueSet);
    }

    @Override
    public int hashCode() {
        return this.liveVarValueSet.hashCode();
    }

    @Override
    public FlexemeDataflowStore copy() {
        return new FlexemeDataflowStore(new HashSet<>(liveVarValueSet));
    }

    @Override
    public FlexemeDataflowStore leastUpperBound(FlexemeDataflowStore other) {
        Set<FlexemeDataflowValue> liveVarValueSetLub =
                new HashSet<>(this.liveVarValueSet.size() + other.liveVarValueSet.size());
        liveVarValueSetLub.addAll(this.liveVarValueSet);
        liveVarValueSetLub.addAll(other.liveVarValueSet);
        return new FlexemeDataflowStore(liveVarValueSetLub);
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
        String key = "live variables";
        if (liveVarValueSet.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (FlexemeDataflowValue liveVarValue : liveVarValueSet) {
            sjStoreVal.add(liveVarValue.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return liveVarValueSet.toString();
    }
}
