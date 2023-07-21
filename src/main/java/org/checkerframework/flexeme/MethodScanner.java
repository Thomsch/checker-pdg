package org.checkerframework.flexeme;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.javacutil.TreeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ExecutableElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MethodScanner extends TreePathScanner<Void, Void> {
    private static final Logger logger = LoggerFactory.getLogger(MethodScanner.class);

    final private Map<MethodTree, ClassTree> methodToClassAstMap;
    private ClassTree classTree;

    public MethodScanner() {
        methodToClassAstMap = new HashMap<>();
    }

    @Override
    public Void visitMethod(MethodTree methodAst, Void p) {
        ExecutableElement el = TreeUtils.elementFromDeclaration(methodAst);

        // If the method is abstract (interface, enum, abstract class), we don't need to build the CFG.
        if (el == null || methodAst.getBody() == null || classTree == null) {
            if (classTree == null) {
                logger.error("Class tree is null");
            }
            return null;
        }

        if (!(methodAst.getName().toString().equals("<init>") && methodAst.getBody().getStatements().size() == 1)) {
            methodToClassAstMap.put(methodAst, classTree);
        }

        return super.visitMethod(methodAst, p);
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
        return methodToClassAstMap.get(method);
    }

    public Map<MethodTree, ClassTree> getMethodToClassAstMap() {
        return methodToClassAstMap;
    }

    public Set<MethodTree> getMethodTrees() {
        return methodToClassAstMap.keySet();
    }
}
