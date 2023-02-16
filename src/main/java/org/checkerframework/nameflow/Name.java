package org.checkerframework.nameflow;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.javacutil.BugInCF;

/**
 * Represents a tuple of name and type from the set returned by the Ξ function in "RefiNym: Using Names to Refine Types".
 * TODO: Store name
 * TODO: Store type.
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

    public enum Kind {AssignL}
}
