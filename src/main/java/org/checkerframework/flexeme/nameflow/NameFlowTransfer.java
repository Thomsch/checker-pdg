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
        TransferResult<NameRecord, NameFlowStore>, TransferInput<NameRecord, NameFlowStore>>
        implements ForwardTransferFunction<NameRecord, NameFlowStore> {

    private static final Logger logger = LoggerFactory.getLogger(NameFlowTransfer.class);

    @Override
    public NameFlowStore initialStore(final UnderlyingAST underlyingAST, final List<LocalVariableNode> parameters) {
        return new NameFlowStore(parameters);
    }

    @Override
    public TransferResult<NameRecord, NameFlowStore> visitNode(final Node node, final TransferInput<NameRecord, NameFlowStore> transferInput) {
        return new RegularTransferResult<>(null, transferInput.getRegularStore());
    }

    @Override
    public TransferResult<NameRecord, NameFlowStore> visitVariableDeclaration(final VariableDeclarationNode n, final TransferInput<NameRecord, NameFlowStore> transferInput) {
        RegularTransferResult<NameRecord, NameFlowStore> transferResult = (RegularTransferResult<NameRecord, NameFlowStore>) super.visitVariableDeclaration(n, transferInput);
        transferResult.getRegularStore().registerVariableDeclaration(n);
        return transferResult;
    }

    @Override
    public TransferResult<NameRecord, NameFlowStore> visitAssignment(final AssignmentNode n, final TransferInput<NameRecord, NameFlowStore> transferInput) {
        RegularTransferResult<NameRecord, NameFlowStore> transferResult = (RegularTransferResult<NameRecord, NameFlowStore>) super.visitAssignment(n, transferInput);
        final Tree variableTree = n.getTarget().getTree();
        if (variableTree == null) {
            // Logging for debugging purposes in case we encounter such a case to determine whether it's an error or not.
            logger.warn("No tree for assigned variable: " + n);
            return transferResult;
        }
        final Element element = TreeUtils.elementFromTree(variableTree);
        if (element == null) {
            // Logging for debugging purposes in case we encounter such a case to determine whether it's an error or not.
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
    public TransferResult<NameRecord, NameFlowStore> visitMethodInvocation(final MethodInvocationNode n, final TransferInput<NameRecord, NameFlowStore> transferInput) {
        RegularTransferResult<NameRecord, NameFlowStore> transferResult = (RegularTransferResult<NameRecord, NameFlowStore>) super.visitMethodInvocation(n, transferInput);
        // Bind parameters to arguments
        for (int i = 0; i < n.getArguments().size(); i++) {
            final Node argument = n.getArguments().get(i); // actual
            final VariableElement parameter = n.getTarget().getMethod().getParameters().get(i); // declared

            // TODO: Convert to visiting the operand through {@link AbstractNodeVisitor}
            // Visitor collects the names, then iterate over the list of names calling `assignM()`, `assignV()`, and `assignL()`.
            assignE(n.getTarget(), argument, transferResult.getRegularStore());
        }
        return transferResult;
    }

    @Override
    public TransferResult<NameRecord, NameFlowStore> visitReturn(final ReturnNode n, final TransferInput<NameRecord, NameFlowStore> transferInput) {
        RegularTransferResult<NameRecord, NameFlowStore> transferResult = (RegularTransferResult<NameRecord, NameFlowStore>) super.visitReturn(n, transferInput);
        transferResult.getRegularStore().addReturnedVariables(n);
        return transferResult;
    }

    /**
     * Implementation of the assignE rule from "RefiNym: Using Names to Refine Types".
     * It recursively visits the expression and stores the variable names encountered.
     *
     * @param target     the left side of the assignment
     * @param expression the right side of the assignment
     * @param store      the store to update
     */
    private void assignE(final Node target, final Node expression, final NameFlowStore store) {
        if (expression instanceof ValueLiteralNode) { //    AssignL
            assignL(target, (ValueLiteralNode) expression, store);
        } else if (expression instanceof LocalVariableNode) {
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
        NameRecord nameRecord = new NameRecord(operand.getTarget().toString(), NameRecord.Kind.Method, operand);
        Element el = TreeUtils.elementFromTree(target.getTree());
        store.add(target, String.valueOf(el.getSimpleName()), nameRecord);
    }

    private void assignV(final Node target, final LocalVariableNode operand, final NameFlowStore store) {
        Element el = TreeUtils.elementFromTree(target.getTree());
        NameRecord nameRecord = new NameRecord(operand.getName(), NameRecord.Kind.Variable, operand);
        store.add(target, String.valueOf(el.getSimpleName()), nameRecord);
    }

    private void assignL(final Node target, final ValueLiteralNode operand, final NameFlowStore store) {
        Object value = operand.getValue();
        if (value == null) { // If the value of the literal is null, then we use the string "null".
            value = "null";
        }
        NameRecord nameRecord = new NameRecord(value.toString(), NameRecord.Kind.Literal, operand);
        Element el = TreeUtils.elementFromTree(target.getTree());
        store.add(target, String.valueOf(el.getSimpleName()), nameRecord);
    }
}
