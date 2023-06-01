package org.checkerframework.flexeme;

import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.checkerframework.org.plumelib.util.IdentityArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import java.util.*;

/**
 * A processor that stores the control flow graph for each method in the file.
 */
@SupportedAnnotationTypes("*")
public class FileProcessor extends BasicTypeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private CompilationUnitTree compilationUnitTree;

    private LineMap lineMap;

    final private Map<MethodTree, ControlFlowGraph> cfgResults;

    private final Map<MethodTree, Set<Node>> cfgNodes;

    private final MethodScanner methodScanner;

    public FileProcessor() {
        cfgResults = new HashMap<>();
        cfgNodes = new HashMap<>();
        methodScanner = new MethodScanner();
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        compilationUnitTree = root;
        lineMap = root.getLineMap();
        return methodScanner;
    }

    public Map<MethodTree, ControlFlowGraph> getMethodCfgs() {
        return cfgResults;
    }

    @Override
    public void typeProcessingOver() {
        // perform analysis for each method.
        for (MethodTree method : methodScanner.getMethodTrees()) {
            final ClassTree classTree = methodScanner.hasClassTree(method);
            if (classTree == null) {
                logger.error("Class tree is null");
            }
            ControlFlowGraph cfg = CFGBuilder.build(compilationUnitTree, method, classTree, processingEnv);

            TreeScanner<Void, Set<Tree>> statementScanner = new StatementScanner(method);
            Set<Tree> statements = new IdentityArraySet<>();
            statementScanner.scan(method, statements);

            System.out.println(cfg.toStringDebug());

            System.out.println("Statements: " + statements.size());
            for (final Tree tree : statements) {
                System.out.println(tree);
            }

            Set<Node> found = new IdentityArraySet<>();
            for (final Tree tree : statements) {
                TreeScanner<Void, Set<Node>> scanner = new TreeScanner<>() {
                    @Override
                    public Void scan(final Tree tree, final Set<Node> found) {
                        // final Set<Node> rec = recursiveScan(tree, cfg);

                        if (tree != null) {
                            final Set<Node> nodes = cfg.getNodesCorrespondingToTree(tree);

                            System.out.println("Tree: " + tree + " " + tree.getClass());


                            if (nodes != null) {
                                System.out.println("Nodes: " + nodes);
                                found.addAll(nodes);
                                for (final Node node : nodes) {
                                    final Collection<Node> transitiveOperands = node.getTransitiveOperands();
                                    found.addAll(transitiveOperands);
                                    System.out.println("   Transitive:" + transitiveOperands);
                                }
                            } else {
                                System.out.println("No nodes for tree");
                            }
                        }
                        return super.scan(tree, found);
                    }

                    private Set<Node> recursiveScan(final Tree tree, final ControlFlowGraph cfg) {
                        final Set<Node> result = new IdentityArraySet<>();
                        final Set<Node> nodes = cfg.getNodesCorrespondingToTree(tree);

                        if (nodes == null) {
                            return result;
                        }
                        result.addAll(nodes);

                        for (final Node node : nodes) {
                            if (node.getTree() != tree) {
                                final Set<Node> newNodes = recursiveScan(node.getTree(), cfg);
                                result.addAll(newNodes);
                            }
                        }
                        return result;
                    }
                };
                scanner.scan(tree, found);
            }
            cfgResults.put(method, cfg);
            cfgNodes.put(method, found);
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

    public Map<MethodTree, Set<Node>> getCfgNodes() {
        return cfgNodes;
    }
}
