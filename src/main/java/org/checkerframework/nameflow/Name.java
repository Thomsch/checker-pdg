package org.checkerframework.nameflow;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.javacutil.BugInCF;

import java.util.Objects;

/**
 * Represents a tuple of name and type from the set returned by the Ξ function in "RefiNym: Using Names to Refine Types".
 */
public class Name implements AbstractValue<Name> {
    private final String name;
    private final Kind kind;

    public Name(final String name, final Kind kind) {
        this.name = name;
        this.kind = kind;
    }

    @Override
    public Name leastUpperBound(final Name name) {
        throw new BugInCF("lub of Name get called!");
    }

    @Override
    public String toString() {
        return "(" + name + "," + kind + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name that = (Name) o;
        return name.equals(that.name) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, kind);
    }

    public enum Kind {Variable, Method, Literal}
}
