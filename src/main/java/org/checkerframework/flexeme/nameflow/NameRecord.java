package org.checkerframework.flexeme.nameflow;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

import java.util.Objects;

/**
 * Represents a tuple of name and type of name from the set returned by the Ξ function in "RefiNym: Using Names to Refine Types".
 * Also contains a unique identifier representing the node in the CFG.
 */
public class NameRecord implements AbstractValue<NameRecord> {

    private final String name;
    private final Kind kind;
    private final Node uid;

    public NameRecord(final String name, final Kind kind, final Node uid) {
        this.name = name;
        this.kind = kind;
        this.uid = uid;
    }

    public Node getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public boolean isLiteral() {
        return kind == Kind.Literal;
    }

    public boolean isMethod() {
        return kind == Kind.Method;
    }

    @Override
    public NameRecord leastUpperBound(final NameRecord nameRecord) {
        throw new BugInCF("lub of NameRecord get called!");
    }

    @Override
    public String toString() {
        return "(" + name + "," + kind + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameRecord that = (NameRecord) o;
        return name.equals(that.name) && kind == that.kind && uid.equals(that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind, uid);
    }

    public enum Kind {Variable, Method, Literal}


}
