package org.checkerframework.flexeme.nameflow;

import com.sun.source.tree.Tree;
import org.checkerframework.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.javacutil.TreeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.util.List;

/**
 * Implementation of the assignment rules from "RefiNym: Using Names to Refine Types".
 * Compared to the paper, the name flow rules encompasses any type, not just scalars.
 */
public class NameFlowTransfer extends AbstractNodeVisitor<
        TransferResult<Name, NameFlowStore>, TransferInput<Name, NameFlowStore>>
        implements ForwardTransferFunction<Name, NameFlowStore> {

    private static final Logger logger = LoggerFactory.getLogger(NameFlowTransfer.class);

    @Override
    public TransferResult<Name, NameFlowStore> visitNode(final Node node, final TransferInput<Name, NameFlowStore> transferInput) {
        return new RegularTransferResult<>(null, transferInput.getRegularStore());
    }

    @Override
    public NameFlowStore initialStore(final UnderlyingAST underlyingAST, final List<LocalVariableNode> list) {
        return new NameFlowStore();
    }

    @Override
    public TransferResult<Name, NameFlowStore> visitAssignment(final AssignmentNode n, final TransferInput<Name, NameFlowStore> transferInput) {
        RegularTransferResult<Name, NameFlowStore> transferResult = (RegularTransferResult<Name, NameFlowStore>) super.visitAssignment(n, transferInput);
        final Tree variableTree = n.getTarget().getTree();
        if (variableTree == null) {
            logger.warn("No tree for assigned variable: " + n);
            return transferResult;
        }
        final Element element = TreeUtils.elementFromTree(variableTree);
        if (element == null) {
            logger.warn("No element for assigned variable: " + n);
            return transferResult;
        }

        for (final Node operand : n.getOperands()) {
            if (operand.equals(n.getTarget())) {
                continue;
            }

            // TODO: Convert to visiting the operand through {@link AbstractNodeVisitor}
            assignE(n.getTarget(), operand, transferResult.getRegularStore());
        }
        return transferResult;
    }

    @Override
    public TransferResult<Name, NameFlowStore> visitMethodInvocation(final MethodInvocationNode n, final TransferInput<Name, NameFlowStore> transferInput) {
        RegularTransferResult<Name, NameFlowStore> transferResult = (RegularTransferResult<Name, NameFlowStore>) super.visitMethodInvocation(n, transferInput);
        // Bind parameters to arguments
        for (int i = 0; i < n.getArguments().size(); i++) {
            final Node argument = n.getArguments().get(i); // actual
            final VariableElement parameter = n.getTarget().getMethod().getParameters().get(i); // declared

            // TODO: Convert to visiting the operand through {@link AbstractNodeVisitor}
            assignE(n.getTarget(), argument, transferResult.getRegularStore());
        }
        return transferResult;
    }

    /**
     * Implementation of the assignE rule from "RefiNym: Using Names to Refine Types".
     * It recursively visits the expression and assigns the names to the element.
     *
     * @param target     The left side of the assignment
     * @param expression The right side of the assignment
     * @param store      The store to update
     */
    private void assignE(final Node target, final Node expression, final NameFlowStore store) {
        if (expression instanceof ValueLiteralNode) { //    AssignL
            assignL(target, (ValueLiteralNode) expression, store);
        } else if (expression instanceof LocalVariableNode){
            assignV(target, (LocalVariableNode) expression, store);
        } else if (expression instanceof MethodInvocationNode) {  //    AssignM
            assignM(target, (MethodInvocationNode) expression, store);
        } else if (expression instanceof UnaryOperationNode) {
            UnaryOperationNode unaryNode = (UnaryOperationNode) expression;
            assignE(target, unaryNode.getOperand(), store);
        } else if (expression instanceof TernaryExpressionNode) {
            TernaryExpressionNode ternaryNode = (TernaryExpressionNode) expression;
            assignE(target, ternaryNode.getConditionOperand(), store);
            assignE(target, ternaryNode.getThenOperand(), store);
            assignE(target, ternaryNode.getElseOperand(), store);
        } else if (expression instanceof TypeCastNode) {
            TypeCastNode typeCastNode = (TypeCastNode) expression;
            assignE(target, typeCastNode.getOperand(), store);
        } else if (expression instanceof InstanceOfNode) {
            InstanceOfNode instanceOfNode = (InstanceOfNode) expression;
            assignE(target, instanceOfNode.getOperand(), store);
        } else if (expression instanceof BinaryOperationNode) {
            BinaryOperationNode binaryNode = (BinaryOperationNode) expression;
            assignE(target, binaryNode.getLeftOperand(), store);
            assignE(target, binaryNode.getRightOperand(), store);
        }
    }

    private void assignM(final Node target, final MethodInvocationNode operand, final NameFlowStore store) {
        Name name = new Name(operand.getTarget().toString(), Name.Kind.Method, "n" + operand.getUid());
        Element el = TreeUtils.elementFromTree(target.getTree());
        store.add("n" + target.getUid(), String.valueOf(el.getSimpleName()), name);
    }

    private void assignV(final Node target, final LocalVariableNode operand, final NameFlowStore store) {
        Element el = TreeUtils.elementFromTree(target.getTree());
        Name name = new Name(operand.getName(), Name.Kind.Variable, "n" + operand.getUid());
        store.add("n" + target.getUid(), String.valueOf(el.getSimpleName()), name);
    }

    private void assignL(final Node target, final ValueLiteralNode operand, final NameFlowStore store) {
        Object value = operand.getValue();
        if (value == null) { // If the value of the literal is null, then we use the string "null".
            value = "null";
        }
        Name name = new Name(value.toString(), Name.Kind.Literal, "n" + operand.getUid());
        Element el = TreeUtils.elementFromTree(target.getTree());
        store.add("n" + target.getUid(),String.valueOf(el.getSimpleName()), name);
    }
}
