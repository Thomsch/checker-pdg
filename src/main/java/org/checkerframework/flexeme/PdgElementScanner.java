package org.checkerframework.flexeme;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;

import java.util.Set;

/**
 * Collects the elements to be included in the PDG for a method. The elements do not include meta nodes such as Entry,
 * Exit, and ExceptionalExit.
 *
 * An PDG element represent nodes in the AST such as statements, and expressions. An PDG element is defined as:
 * <ul>
 *     <li>a outermost expression (e.g., a > b || c < b)</li>
 *     <li>a variable declaration (e.g., int i = 0)</li>
 *     <li>an assignment (e.g., a = b)</li>
 *     <li>a method invocation (e.g., System.out.println("Hello"))</li>
 *     <li>a return statement (e.g., return a)</li>
 * </ul>
 *
 * AST nodes representing control flow structures (e.g., if, for, while) are not PDG elements.
 * However, their conditional expressions and body's statements are PDG elements.
 *
 */
class PdgElementScanner extends TreeScanner<Void, Set<Tree>> {

    private final MethodTree method;

    public PdgElementScanner(final MethodTree method) {
        this.method = method;
    }

    @Override
    public Void visitIf(final IfTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getCondition());
        return super.visitIf(node, pdgElements);
    }

    @Override
    public Void visitCompoundAssignment(final CompoundAssignmentTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return super.visitCompoundAssignment(node, pdgElements);
    }

    @Override
    public Void visitParenthesized(final ParenthesizedTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getExpression());
        return super.visitParenthesized(node, pdgElements);
    }

    @Override
    public Void visitForLoop(final ForLoopTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getCondition());
        return super.visitForLoop(node, pdgElements);
    }

    @Override
    public Void visitEnhancedForLoop(final EnhancedForLoopTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getVariable());
        pdgElements.add(node.getExpression());
        return super.visitEnhancedForLoop(node, pdgElements);
    }

    @Override
    public Void visitReturn(final ReturnTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return super.visitReturn(node, pdgElements);
    }

    @Override
    public Void visitMethodInvocation(final MethodInvocationTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return super.visitMethodInvocation(node, pdgElements);
    }

    @Override
    public Void visitAssignment(final AssignmentTree node, final Set<Tree> trees) {
        trees.add(node);
        return super.visitAssignment(node, trees);
    }

    @Override
    public Void visitVariable(final VariableTree node, final Set<Tree> pdgElements) {
        if (!method.getParameters().contains(node)) {
            // System.out.println("Variable: " + node);
            pdgElements.add(node);
        }
        return super.visitVariable(node, pdgElements);
    }

    @Override
    public Void visitUnary(final UnaryTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return super.visitUnary(node, pdgElements);
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatementTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getExpression());
        return super.visitExpressionStatement(node, pdgElements);
    }
}
