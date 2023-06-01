package org.checkerframework.flexeme;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;

import java.util.Set;

/**
 * A scanner that collects all statements in a method for a Flexeme PDG graph.
 */
class StatementScanner extends TreeScanner<Void, Set<Tree>> {

    private final MethodTree method;

    public StatementScanner(final MethodTree method) {
        this.method = method;
    }

    @Override
    public Void visitIf(final IfTree node, final Set<Tree> statements) {
        statements.add(node.getCondition());
        return super.visitIf(node, statements);
    }

    @Override
    public Void visitCompoundAssignment(final CompoundAssignmentTree node, final Set<Tree> statements) {
        statements.add(node);
        return super.visitCompoundAssignment(node, statements);
    }

    @Override
    public Void visitParenthesized(final ParenthesizedTree node, final Set<Tree> statements) {
        statements.add(node.getExpression());
        return super.visitParenthesized(node, statements);
    }

    @Override
    public Void visitForLoop(final ForLoopTree node, final Set<Tree> statements) {
        statements.add(node.getCondition());
        return super.visitForLoop(node, statements);
    }

    @Override
    public Void visitReturn(final ReturnTree node, final Set<Tree> statements) {
        statements.add(node);
        return super.visitReturn(node, statements);
    }

    @Override
    public Void visitMethodInvocation(final MethodInvocationTree node, final Set<Tree> statements) {
        statements.add(node);
        return super.visitMethodInvocation(node, statements);
    }

    @Override
    public Void visitAssignment(final AssignmentTree node, final Set<Tree> trees) {
        trees.add(node);
        return super.visitAssignment(node, trees);
    }

    @Override
    public Void visitVariable(final VariableTree node, final Set<Tree> statements) {
        if (!method.getParameters().contains(node)) {
            // System.out.println("Variable: " + node);
            statements.add(node);
        }
        return super.visitVariable(node, statements);
    }

    @Override
    public Void visitUnary(final UnaryTree node, final Set<Tree> statements) {
        statements.add(node);
        return super.visitUnary(node, statements);
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatementTree node, final Set<Tree> statements) {
        statements.add(node.getExpression());
        return super.visitExpressionStatement(node, statements);
    }
}
