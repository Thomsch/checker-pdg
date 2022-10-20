package org.checkerframework.checker.codechanges;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

public class FlexemeDataflowVisitor extends BaseTypeVisitor<FlexemeDataflowAnnotatedTypeFactory> {

    public FlexemeDataflowVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        System.out.println("Hello Variable " + node.getName());
        System.out.println("-> " + TreeUtils.elementFromDeclaration(node));

        return super.visitVariable(node, p);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        System.out.println("I'm identifier " + node);
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        System.out.println("Hello Assignment");
        return super.visitAssignment(node, p);
    }
}
