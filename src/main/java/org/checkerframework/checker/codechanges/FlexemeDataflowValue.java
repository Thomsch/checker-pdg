package org.checkerframework.checker.codechanges;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

public class FlexemeDataflowValue implements AbstractValue<FlexemeDataflowValue> {
    /**
     * A live variable is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.LocalVariableNode} or {@link
     * org.checkerframework.dataflow.cfg.node.FieldAccessNode}.
     */
    protected final Node liveVariable;

    @Override
    public FlexemeDataflowValue leastUpperBound(FlexemeDataflowValue other) {
        throw new BugInCF("lub of LiveVar get called!");
    }

    /**
     * Create a new live variable.
     *
     * @param n a node
     */
    public FlexemeDataflowValue(Node n) {
        this.liveVariable = n;
    }

    @Override
    public int hashCode() {
        return this.liveVariable.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof FlexemeDataflowValue)) {
            return false;
        }
        FlexemeDataflowValue other = (FlexemeDataflowValue) obj;
        return this.liveVariable.equals(other.liveVariable);
    }

    @Override
    public String toString() {
        return this.liveVariable.toString();
    }
}
