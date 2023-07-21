package org.checkerframework.flexeme;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Processor for the file compilation. Stores the ASTs of the methods
 * in the file and the line map of the compilation unit.
 */
@SupportedAnnotationTypes("*")
public class FileProcessor extends BasicTypeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private final MethodScanner methodScanner;
    private final Map<MethodTree, ControlFlowGraph> methodAstToCfgMap;
    private LineMap lineMap;
    private EndPosTable endPosTable;

    public FileProcessor() {
        methodScanner = new MethodScanner();
        methodAstToCfgMap = new HashMap<>();
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        if (root instanceof JCTree.JCCompilationUnit) {
            JCTree.JCCompilationUnit jcCompilationUnit = (JCTree.JCCompilationUnit) root;
            endPosTable = jcCompilationUnit.endPositions;
        } else {
            logger.warn("CompilationUnitTree is not an instance of JCTree.JCCompilationUnit");
        }

        lineMap = root.getLineMap();
        return methodScanner;
    }

    @Override
    public void typeProcessingOver() {
        methodScanner.getMethodToClassAstMap().forEach((methodTree, classTree) -> {
            // The CFG builder has to be called in a {@link Processor}. Calling the builder outside a processor
            // throws an exception because the Java compiler is terminated.
            final ControlFlowGraph methodCfg = CFGBuilder.build(currentRoot, methodTree, classTree, processingEnv);
            methodAstToCfgMap.put(methodTree, methodCfg);
        });

        super.typeProcessingOver();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    public LineMap getLineMap() {
        return lineMap;
    }

    public EndPosTable getEndPosTable() {
        return endPosTable;
    }

    public ClassTree getClassTree(final MethodTree methodTree) {
        return methodScanner.getMethodToClassAstMap().get(methodTree);
    }

    public Set<MethodTree> getMethodsAst() {
        return methodScanner.getMethodTrees();
    }

    /**
     * Returns the method tree with the given name.
     * If multiple methods with the same name exists, the first one is returned.
     *
     * @param methodName the name of the method to find.
     * @return the method tree with the given name.
     * @throws NoSuchElementException if no method with the given name exists.
     */
    public MethodTree getMethod(final String methodName) {
        for (final MethodTree methodTree : methodScanner.getMethodTrees()) {
            if (methodTree.getName().toString().equals(methodName)) {
                return methodTree;
            }
        }
        throw new NoSuchElementException("No method with name " + methodName + " exists.");
    }

    public CompilationUnitTree getCompilationUnitTree() {
        return currentRoot;
    }

    public ProcessingEnvironment getProcessingEnvironment() {
        return processingEnv;
    }

    public ControlFlowGraph getMethodCfg(final MethodTree methodAst) {
        return methodAstToCfgMap.get(methodAst);
    }
}
