package org.checkerframework.checker.codechanges;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.dataflow.cfg.CFGProcessor;

import javax.annotation.processing.SupportedAnnotationTypes;

@SupportedAnnotationTypes("*")
public class LineMapCFGProcessor extends CFGProcessor {

    private LineMap lineMap;
    private CompilationUnitTree root;

    public LineMapCFGProcessor(String clas, String method) {
        super(clas, method);
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        lineMap = root.getLineMap();
        this.root = root;
        return super.createTreePathScanner(root);
    }

    public LineMap getLineMap() {
        return lineMap;
    }

    public CompilationUnitTree getRoot() {
        return root;
    }
}
