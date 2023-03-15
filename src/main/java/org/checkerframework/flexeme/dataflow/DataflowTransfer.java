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
        TransferResult<DataflowValue, DataflowStore>, TransferInput<DataflowValue, DataflowStore>>
        implements ForwardTransferFunction<DataflowValue, DataflowStore> {

    @Override
    public DataflowStore initialStore(UnderlyingAST underlyingAST, List<LocalVariableNode> parameters) {
        return new DataflowStore(parameters);
    }

    @Override
    public RegularTransferResult<DataflowValue, DataflowStore> visitNode(
            Node n, TransferInput<DataflowValue, DataflowStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public TransferResult<DataflowValue, DataflowStore> visitVariableDeclaration(VariableDeclarationNode n, TransferInput<DataflowValue, DataflowStore> p) {
        RegularTransferResult<DataflowValue, DataflowStore> transferResult =
                (RegularTransferResult<DataflowValue, DataflowStore>) super.visitVariableDeclaration(n, p);
        transferResult.getRegularStore().addLocalVariableDeclaration(n);
        return transferResult;
    }

    @Override
    public TransferResult<DataflowValue, DataflowStore> visitLocalVariable(LocalVariableNode n, TransferInput<DataflowValue, DataflowStore> p) {
        RegularTransferResult<DataflowValue, DataflowStore> transferResult =
                (RegularTransferResult<DataflowValue, DataflowStore>) super.visitLocalVariable(n, p);
        transferResult.getRegularStore().addDataflowEdge(n);
        return transferResult;
    }

}
