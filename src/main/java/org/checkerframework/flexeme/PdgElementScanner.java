package org.checkerframework.flexeme;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;

import java.util.Set;

/**
 * Collects the elements to be included in the PDG for a method. The elements do not include meta nodes such as Entry,
 * Exit, and ExceptionalExit.
 * <p>
 * An PDG element represent nodes in the AST such as statements, and expressions. An PDG element is defined as:
 * <ul>
 *     <li>a outermost expression (e.g., a > b || c < b)</li>
 *     <li>a variable declaration (e.g., int i = 0)</li>
 *     <li>an assignment (e.g., a = b)</li>
 *     <li>a method invocation (e.g., System.out.println("Hello"))</li>
 *     <li>a return statement (e.g., return a)</li>
 * </ul>
 * <p>
 * AST nodes representing control flow structures (e.g., if, for, while) are not PDG elements.
 * However, their conditional expressions and body's statements are PDG elements.
 */
public class PdgElementScanner extends TreeScanner<Void, Set<Tree>> {

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
    public Void visitForLoop(final ForLoopTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getCondition());
        return super.visitForLoop(node, pdgElements);
    }

    @Override
    public Void visitSwitch(final SwitchTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getExpression());
        return super.visitSwitch(node, pdgElements);
    }

    @Override
    public Void visitEnhancedForLoop(final EnhancedForLoopTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getVariable());
        pdgElements.add(node.getExpression());
        return super.visitEnhancedForLoop(node, pdgElements);
    }

    @Override
    public Void visitCompoundAssignment(final CompoundAssignmentTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return null;
    }

    @Override
    public Void visitThrow(final ThrowTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return null;
    }

    @Override
    public Void visitReturn(final ReturnTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return null;
    }

    @Override
    public Void visitMethodInvocation(final MethodInvocationTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return null;
    }

    @Override
    public Void visitAssignment(final AssignmentTree node, final Set<Tree> trees) {
        trees.add(node);
        return null;
    }

    @Override
    public Void visitVariable(final VariableTree node, final Set<Tree> pdgElements) {
        if (!method.getParameters().contains(node)) { // exclude method parameters
            pdgElements.add(node);
        }
        return null;
    }

    @Override
    public Void visitUnary(final UnaryTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node);
        return null;
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatementTree node, final Set<Tree> pdgElements) {
        pdgElements.add(node.getExpression());
        return null;
    }
}
