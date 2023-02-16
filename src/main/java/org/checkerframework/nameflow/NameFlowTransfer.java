package org.checkerframework.nameflow;

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
import javax.lang.model.type.TypeKind;
import java.util.List;

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
        // For each operand in the expression, add the name to the store

        // operand can be anything from a literal to a method call.

        RegularTransferResult<Name, NameFlowStore> transferResult = (RegularTransferResult<Name, NameFlowStore>) super.visitAssignment(n, transferInput);
        if (isScalar(n.getExpression().getType().getKind())) {
            // System.out.println("visitAssignment: " + n);
            // System.out.println("operands: " + n.getOperands());
            // System.out.println("target: " + n.getTarget());
            // System.out.println("expression: " + n.getExpression());
            // System.out.println(n.getExpression().getOperands());
            // System.out.println(n.getExpression().getTransitiveOperands());

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

                if (operand instanceof ValueLiteralNode) { //    AssignL
                    assignL(element, (ValueLiteralNode) operand, transferResult.getRegularStore());
                } else if (operand instanceof LocalVariableNode && !operand.equals(n.getTarget())) { //    AssignV
                    assignV(element, (LocalVariableNode) operand, transferResult.getRegularStore());
                } else if (operand instanceof MethodInvocationNode) {  //    AssignM
                    assignM(element, (MethodInvocationNode) operand, transferResult.getRegularStore());
                } else {
                    assignE(element, operand, transferResult.getRegularStore());
                }
            }
        }
        return transferResult;
    }

    private void assignE(final Element element, final Node expression, final NameFlowStore store) {
        if (expression instanceof ValueLiteralNode) { //    AssignL
            assignL(element, (ValueLiteralNode) expression, store);
        } else if (expression instanceof LocalVariableNode){
            assignV(element, (LocalVariableNode) expression, store);
        } else if (expression instanceof MethodInvocationNode) {  //    AssignM
            assignM(element, (MethodInvocationNode) expression, store);
        } else if (expression instanceof UnaryOperationNode) {
            UnaryOperationNode unaryNode = (UnaryOperationNode) expression;
            assignE(element, unaryNode.getOperand(), store);
        } else if (expression instanceof TernaryExpressionNode) {
            TernaryExpressionNode ternaryNode = (TernaryExpressionNode) expression;
            assignE(element, ternaryNode.getConditionOperand(), store);
            assignE(element, ternaryNode.getThenOperand(), store);
            assignE(element, ternaryNode.getElseOperand(), store);
        } else if (expression instanceof TypeCastNode) {
            TypeCastNode typeCastNode = (TypeCastNode) expression;
            assignE(element, typeCastNode.getOperand(), store);
        } else if (expression instanceof InstanceOfNode) {
            InstanceOfNode instanceOfNode = (InstanceOfNode) expression;
            assignE(element, instanceOfNode.getOperand(), store);
        } else if (expression instanceof BinaryOperationNode) {
            BinaryOperationNode binaryNode = (BinaryOperationNode) expression;
            assignE(element, binaryNode.getLeftOperand(), store);
            assignE(element, binaryNode.getRightOperand(), store);
        }
    }

    private void assignM(final Element element, final MethodInvocationNode operand, final NameFlowStore store) {
        Name name = new Name(operand.getTarget().toString(), Name.Kind.AssignM);
        store.add(element.getSimpleName().toString(), name);
    }

    private void assignV(final Element element, final LocalVariableNode operand, final NameFlowStore store) {
        Name name = new Name(operand.getName(), Name.Kind.AssignV);
        store.add(element.getSimpleName().toString(), name);
    }

    private void assignL(final Element element, final ValueLiteralNode operand, final NameFlowStore store) {
        Name name = new Name(operand.getValue().toString(), Name.Kind.AssignL);
        store.add(element.getSimpleName().toString(), name);
    }

    private static boolean isScalar(final TypeKind type) {
        return type == TypeKind.INT
                || type == TypeKind.LONG
                || type == TypeKind.SHORT
                || type == TypeKind.FLOAT
                || type == TypeKind.DOUBLE;
    }

    @Override
    public TransferResult<Name, NameFlowStore> visitMethodInvocation(final MethodInvocationNode n, final TransferInput<Name, NameFlowStore> transferInput) {
        System.out.println("visitMethodInvocation:" + n.getTarget() + " = " + n.getArguments());
        System.out.println(n.getTree());
        // Invoke
        return super.visitMethodInvocation(n, transferInput);
    }

}
