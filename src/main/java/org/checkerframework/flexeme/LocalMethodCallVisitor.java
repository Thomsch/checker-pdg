package org.checkerframework.flexeme;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreeScanner;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.ExecutableElement;
import java.util.HashSet;
import java.util.Set;

/**
 * A scanner that retrieves method calls in a PDG element.
 */
public class LocalMethodCallVisitor extends TreeScanner<Set<ExecutableElement>, Void> {

    @Override
    public Set<ExecutableElement> visitMethodInvocation(final MethodInvocationTree node, final Void ignore) {
        HashSet<ExecutableElement> result = new HashSet<>();
        result.add(TreeUtils.elementFromUse(node));

        Set<ExecutableElement> resultChildren = super.visitMethodInvocation(node, ignore);
        if (resultChildren != null) {
            result.addAll(resultChildren);
        }
        return result;
    }

    @Override
    public Set<ExecutableElement> reduce(final Set<ExecutableElement> r1, final Set<ExecutableElement> r2) {
        if (r1 == null && r2 == null) {
            return new HashSet<>();
        } else if (r1 == null) {
            return r2;
        } else if (r2 == null) {
            return r1;
        } else {
            r1.addAll(r2);
            return r1;
        }
    }
}
