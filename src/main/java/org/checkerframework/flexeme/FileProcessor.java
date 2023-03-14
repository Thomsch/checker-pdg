package org.checkerframework.flexeme;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.checkerframework.javacutil.TreeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SupportedAnnotationTypes("*")
public class FileProcessor extends BasicTypeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);
    final private List<MethodTree> methodTrees;

    private ClassTree classTree;

    private CompilationUnitTree rootTree;

    private LineMap lineMap;

    final private Map<MethodTree, ControlFlowGraph> cfgResults;

    final private Map<MethodTree, ClassTree> classMap;

    public FileProcessor() {
        methodTrees = new ArrayList<>();
        cfgResults = new HashMap<>();
        classMap = new HashMap<>();
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        rootTree = root;
        lineMap = root.getLineMap();

        return new TreePathScanner<Void, Void>() {
            @Override
            public Void visitMethod(MethodTree node, Void p) {
                ExecutableElement el = TreeUtils.elementFromDeclaration(node);

                // If the method is abstract (interface, enum, abstract class), we don't need to build the CFG.
                if (el == null || node.getBody() == null || classTree == null) {
                    return null;
                }

                methodTrees.add(node);
                classMap.put(node, classTree);
                return null;
            }

            @Override
            public Void visitClass(ClassTree node, Void unused) {
                // We update the current class tree visited, so the method visitor has this information when building the CFG.
                // `node` may be null in some cases.
                if (node == null) {
                    logger.error("Class tree is null!");
                }
                classTree = node;
                return super.visitClass(node, unused);
            }
        };
    }

    public Map<MethodTree, ControlFlowGraph> getMethodCfgs() {
        return cfgResults;
    }

    @Override
    public void typeProcessingOver() {
        // perform analysis for each method.
        for (MethodTree method : methodTrees) {
            final ClassTree classTree = classMap.get(method);
            if (classTree == null) {
                logger.error("Class tree is null");
            }
            ControlFlowGraph cfg = CFGBuilder.build(rootTree, method, classTree, processingEnv);
            cfgResults.put(method, cfg);
        }

        super.typeProcessingOver();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public LineMap getLineMap() {
        return lineMap;
    }
}
