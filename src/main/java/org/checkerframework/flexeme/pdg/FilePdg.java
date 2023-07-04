package org.checkerframework.flexeme.pdg;

import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.ExecutableElement;
import java.util.Set;

/**
 * Represents the PDGs for the methods of a file and the method call edges between them.
 */
public class FilePdg {
    private final Set<MethodPdg> graphs;
    private final Set<PdgEdge> localCalls;

    public FilePdg(final Set<MethodPdg> graphs, final Set<PdgEdge> localCalls) {
        this.graphs = graphs;
        this.localCalls = localCalls;
    }

    /**
     * Returns the set of PDGs for the methods of a file.
     * @return the set of PDGs
     */
    public Set<MethodPdg> getGraphs() {
        return graphs;
    }

    /**
     * Returns the set of method call edges between methods in the same class.
     * @return the set of method call
     */
    public Set<PdgEdge> getLocalCalls() {
        return localCalls;
    }

    /**
     * Returns whether the given PDG element is calling a method in the same class.
     * @param expectedFromQualifiedMethodName The PDG element's qualified method name
     * @param expectedFromPdgElementLabel The PDG element's label
     * @param expectedToQualifiedMethodName The expected qualified method name of the called method
     * @return true if the PDG element is calling the expected method, false otherwise
     */
    public boolean containsCall(final String expectedFromQualifiedMethodName, final String expectedFromPdgElementLabel, final String expectedToQualifiedMethodName) {
        for (final PdgEdge localCall : localCalls) {

            final MethodPdg from = localCall.from.getPdg();
            final MethodPdg to = localCall.to.getPdg();

            final ExecutableElement fromElement = TreeUtils.elementFromDeclaration(from.getTree());
            final String fromQualifiedName = ElementUtils.getQualifiedName(fromElement);


            final ExecutableElement toElement = TreeUtils.elementFromDeclaration(to.getTree());
            final String toQualifiedName = ElementUtils.getQualifiedName(toElement);

            if (expectedFromQualifiedMethodName.equals(fromQualifiedName)
                && expectedFromPdgElementLabel.equals(localCall.from.toString())
                && expectedToQualifiedMethodName.equals(toQualifiedName)) {
                return true;
            }
        }
        return false;
    }
}
