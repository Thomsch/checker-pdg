package org.checkerframework.flexeme;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.javacutil.TreeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodScanner extends TreePathScanner<Void, Void> {
    private static final Logger logger = LoggerFactory.getLogger(MethodScanner.class);

    final private List<MethodTree> methodTrees;

    final private Map<MethodTree, ClassTree> classMap;
    private ClassTree classTree;

    public MethodScanner() {
        methodTrees = new ArrayList<>();
        classMap = new HashMap<>();
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        ExecutableElement el = TreeUtils.elementFromDeclaration(node);

        // If the method is abstract (interface, enum, abstract class), we don't need to build the CFG.
        if (el == null || node.getBody() == null || classTree == null) {
            return null;
        }

        methodTrees.add(node);
        classMap.put(node, classTree);

        return super.visitMethod(node, p);
    }

    @Override
    public Void visitClass(ClassTree node, Void unused) {
        // We update the current class tree visited, so the method visitor has this information when building the CFG.
        // `node` may be null in some cases.
        if (node == null) {
            logger.error("Class tree is null!");
        }
        classTree = node;
        return super.visitClass(node, unused);
    }

    public ClassTree hasClassTree(final MethodTree method) {
        return classMap.get(method);
    }

    public List<MethodTree> getMethodTrees() {
        return methodTrees;
    }
}
