package org.checkerframework.checker.codechanges;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SimpleTreeVisitor;
import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.*;

import java.util.List;

public class FlexemeDataflowTransfer extends AbstractNodeVisitor<
            TransferResult<FlexemeDataflowValue, FlexemeDataflowStore>, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore>>
    implements BackwardTransferFunction<FlexemeDataflowValue, FlexemeDataflowStore> {

        @Override
        public FlexemeDataflowStore initialNormalExitStore(
                UnderlyingAST underlyingAST, List<ReturnNode> returnNodes) {
            System.out.println("ASDSAD");
            return new FlexemeDataflowStore();
        }

        @Override
        public FlexemeDataflowStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
            return new FlexemeDataflowStore();
        }

        @Override
        public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitNode(
                Node n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {

            if (n instanceof LocalVariableNode) {
                LocalVariableNode localVariable = (LocalVariableNode) n;
                System.out.println("Local variable: " + localVariable);
            }
            return new RegularTransferResult<>(null, p.getRegularStore());
        }

        @Override
        public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitAssignment(
                AssignmentNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
            RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                    (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitAssignment(n, p);
            processLiveVarInAssignment(n.getTarget(), n.getExpression(), transferResult.getRegularStore());
            return transferResult;
        }

        @Override
        public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitStringConcatenateAssignment(
                StringConcatenateAssignmentNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
            RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                    (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>)
                            super.visitStringConcatenateAssignment(n, p);
            processLiveVarInAssignment(
                    n.getLeftOperand(), n.getRightOperand(), transferResult.getRegularStore());
            return transferResult;
        }

        @Override
        public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitMethodInvocation(
                MethodInvocationNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
            RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                    (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitMethodInvocation(n, p);
            FlexemeDataflowStore store = transferResult.getRegularStore();
            for (Node arg : n.getArguments()) {
                store.addUseInExpression(arg);
            }
            return transferResult;
        }

        @Override
        public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitObjectCreation(
                ObjectCreationNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
            RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                    (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitObjectCreation(n, p);
            FlexemeDataflowStore store = transferResult.getRegularStore();
            for (Node arg : n.getArguments()) {
                store.addUseInExpression(arg);
            }
            return transferResult;
        }

        @Override
        public RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> visitReturn(
                ReturnNode n, TransferInput<FlexemeDataflowValue, FlexemeDataflowStore> p) {
            RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore> transferResult =
                    (RegularTransferResult<FlexemeDataflowValue, FlexemeDataflowStore>) super.visitReturn(n, p);
            Node result = n.getResult();
            if (result != null) {
                FlexemeDataflowStore store = transferResult.getRegularStore();
                store.addUseInExpression(result);
            }
            return transferResult;
        }

        /**
         * Update the information of live variables from an assignment statement.
         *
         * @param variable the variable that should be killed
         * @param expression the expression in which the variables should be added
         * @param store the live variable store
         */
        private void processLiveVarInAssignment(Node variable, Node expression, FlexemeDataflowStore store) {
            store.killLiveVar(new FlexemeDataflowValue(variable));
            store.addUseInExpression(expression);
        }
}
