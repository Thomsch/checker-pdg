package org.checkerframework.checker.codechanges;

import org.checkerframework.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.VariableDeclarationNode;

import java.util.List;

public class FlexemeDataflowTransfer extends AbstractNodeVisitor<
        TransferResult<FlexemeDataflowValue, FlexemeDataflowStore>, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore>>
        implements ForwardTransferFunction<FlexemeDataflowValue, FlexemeDataflowStore> {

    @Override
    public FlexemeDataflowStore initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
        return new FlexemeDataflowStore(parameters);
    }

    @Override
    public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitNode(
            Node n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public TransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitVariableDeclaration(VariableDeclarationNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitVariableDeclaration(n, p);
        transferResult.getRegularStore().addLocalVariableDeclaration(n);
        return transferResult;
    }

    @Override
    public TransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitLocalVariable(LocalVariableNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitLocalVariable(n, p);
        transferResult.getRegularStore().addDataflowEdge(n);
        return transferResult;
    }

    //    @Override
    //    public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitAssignment(
    //            AssignmentNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
    //        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
    //                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitAssignment(n, p);
    //        System.out.println("Assigned: " + n);
    //        processLiveVarInAssignment(n.getTarget(), n.getExpression(), transferResult.getRegularStore());
    //        return transferResult;
    //    }

    //    @Override
    //    public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitStringConcatenateAssignment(
    //            StringConcatenateAssignmentNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
    //        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
    //                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>)
    //                        super.visitStringConcatenateAssignment(n, p);
    //        processLiveVarInAssignment(
    //                n.getLeftOperand(), n.getRightOperand(), transferResult.getRegularStore());
    //        return transferResult;
    //    }
    //
    //    @Override
    //    public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitMethodInvocation(
    //            MethodInvocationNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
    //        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
    //                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitMethodInvocation(n, p);
    //        FlexemeDataflowStore store = transferResult.getRegularStore();
    //        for (Node arg : n.getArguments()) {
    //            store.addUseInExpression(arg);
    //        }
    //        return transferResult;
    //    }

    //    @Override
    //    public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitObjectCreation(
    //            ObjectCreationNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
    //        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
    //                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitObjectCreation(n, p);
    //        FlexemeDataflowStore store = transferResult.getRegularStore();
    //        for (Node arg : n.getArguments()) {
    //            store.addUseInExpression(arg);
    //        }
    //        return transferResult;
    //    }

    //    @Override
    //    public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitReturn(
    //            ReturnNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
    //        RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
    //                (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitReturn(n, p);
    //        Node result = n.getResult();
    //        if (result != null) {
    //            FlexemeDataflowStore store = transferResult.getRegularStore();
    //            store.addUseInExpression(result);
    //        }
    //        return transferResult;
    //    }

}
