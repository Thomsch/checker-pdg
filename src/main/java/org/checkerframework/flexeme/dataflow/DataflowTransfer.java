package org.checkerframework.flexeme.dataflow;

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

/**
 * Transfer rules for the dataflow analysis.
 */
public class DataflowTransfer extends AbstractNodeVisitor<
        TransferResult<VariableReference, DataflowStore>, TransferInput<VariableReference, DataflowStore>>
        implements ForwardTransferFunction<VariableReference, DataflowStore> {

    @Override
    public DataflowStore initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
        return new DataflowStore(parameters);
    }

    @Override
    public RegularTransferResult<VariableReference, DataflowStore> visitNode(
            Node n, TransferInput<VariableReference, DataflowStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public TransferResult<VariableReference, DataflowStore> visitVariableDeclaration(VariableDeclarationNode n, TransferInput<VariableReference, DataflowStore> p) {
        RegularTransferResult<VariableReference, DataflowStore> transferResult =
                (RegularTransferResult<VariableReference, DataflowStore>) super.visitVariableDeclaration(n, p);
        transferResult.getRegularStore().addLocalVariableDeclaration(n);
        return transferResult;
    }

    @Override
    public TransferResult<VariableReference, DataflowStore> visitLocalVariable(LocalVariableNode n, TransferInput<VariableReference, DataflowStore> p) {
        RegularTransferResult<VariableReference, DataflowStore> transferResult =
                (RegularTransferResult<VariableReference, DataflowStore>) super.visitLocalVariable(n, p);
        transferResult.getRegularStore().addDataflowEdge(n);
        return transferResult;
    }

}
