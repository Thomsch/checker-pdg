package org.checkerframework.checker.codechanges;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;

public class FlexemeDataflowAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    public FlexemeDataflowAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected void postInit() {
        super.postInit();
        System.out.println("Post Init");
    }

    @Override
    protected void postAnalyze(ControlFlowGraph cfg) {
        super.postAnalyze(cfg);
        System.out.println("Post Analyze");
    }
}
