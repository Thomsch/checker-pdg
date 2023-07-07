package org.checkerframework.flexeme.dataflow;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

import java.util.Objects;

/**
 * Represents a variable reference.
 */
public class VariableReference implements AbstractValue<VariableReference> {
    /**
     * A value is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.LocalVariableNode} or {@link
     * org.checkerframework.dataflow.cfg.node.FieldAccessNode}.
     */
    protected final Node reference;

    /**
     * Create a new live variable.
     *
     * @param n a node
     */
    public VariableReference(Node n) {
        this.reference = n;
    }

    @Override
    public VariableReference leastUpperBound(VariableReference other) {
        throw new BugInCF("lub of LiveVar get called!");
    }

    public Node getReference() {
        return reference;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.reference.getTree());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof VariableReference)) {
            return false;
        }
        VariableReference other = (VariableReference) obj;
        return Objects.equals(this.reference.getTree(), other.reference.getTree());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(reference.getTree().toString());
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
