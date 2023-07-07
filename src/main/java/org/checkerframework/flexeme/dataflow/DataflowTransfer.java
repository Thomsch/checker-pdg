package org.checkerframework.flexeme.dataflow;

import org.checkerframework.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.*;

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
    public TransferResult<VariableReference, DataflowStore> visitAssignment(final AssignmentNode node, final TransferInput<VariableReference, DataflowStore> transferInput) {
        RegularTransferResult<VariableReference, DataflowStore> transferResult =
                (RegularTransferResult<VariableReference, DataflowStore>) super.visitAssignment(node, transferInput);
        transferResult.getRegularStore().registerAssignment(node);
        return transferResult;
    }

    @Override
    public TransferResult<VariableReference, DataflowStore> visitLocalVariable(LocalVariableNode node, TransferInput<VariableReference, DataflowStore> transferInput) {
        RegularTransferResult<VariableReference, DataflowStore> transferResult =
                (RegularTransferResult<VariableReference, DataflowStore>) super.visitLocalVariable(node, transferInput);
        transferResult.getRegularStore().addDataflowEdge(node);
        return transferResult;
    }

}
