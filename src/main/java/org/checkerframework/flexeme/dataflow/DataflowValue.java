package org.checkerframework.flexeme.dataflow;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

public class DataflowValue implements AbstractValue<DataflowValue> {
    /**
     * A live variable is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.LocalVariableNode} or {@link
     * org.checkerframework.dataflow.cfg.node.FieldAccessNode}.
     */
    protected final Node reference;

    @Override
    public DataflowValue leastUpperBound(DataflowValue other) {
        throw new BugInCF("lub of LiveVar get called!");
    }

    /**
     * Create a new live variable.
     *
     * @param n a node
     */
    public DataflowValue(Node n) {
        this.reference = n;
    }

    public Node getReference() {
        return reference;
    }

    @Override
    public int hashCode() {
        return this.reference.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof DataflowValue)) {
            return false;
        }
        DataflowValue other = (DataflowValue) obj;
        return this.reference.equals(other.reference) && this.reference.getUid() == other.reference.getUid();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(reference.toString());
        s.append("[");
        s.append(reference.getClass().getSimpleName());

        if (this.reference.getTree() != null) {
            s.append(",");
            s.append(this.reference.getTree().getKind());
        }
        s.append("] ");
        s.append("(uuid=");
        s.append(this.reference.getUid());
        s.append(")");
        return s.toString();
    }
}
