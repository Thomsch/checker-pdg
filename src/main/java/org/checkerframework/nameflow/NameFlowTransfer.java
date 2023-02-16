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
        RegularTransferResult<Name, NameFlowStore> transferResult = (RegularTransferResult<Name, NameFlowStore>) super.visitAssignment(n, transferInput);
        if (isScalar(n.getExpression().getType().getKind())) {
            for (final Node operand : n.getOperands()) {
                //    AssignL
                if (operand instanceof ValueLiteralNode) {
                    assignL(n, (ValueLiteralNode) operand, transferResult.getRegularStore());
                }
            }
        //    AssignV
        //    AssignM
        //    AssignE
        }
        return transferResult;
    }

    private void assignL(final AssignmentNode n, final ValueLiteralNode operand, final NameFlowStore store) {
        final Tree variableTree = n.getTarget().getTree();
        if (variableTree == null) {
            logger.warn("No tree for assigned variable: " + n);
            return;
        }
        final Element element = TreeUtils.elementFromTree(variableTree);
        if (element == null) {
            logger.warn("No element for assigned variable: " + n);
            return;
        }
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
        // System.out.println("visitMethodInvocation:" + n.getTarget() + " = " + n.getArguments());
        // Invoke
        return super.visitMethodInvocation(n, transferInput);
    }

}
