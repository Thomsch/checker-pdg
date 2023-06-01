package org.checkerframework.flexeme;

import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BasicTypeProcessor;
import org.checkerframework.org.plumelib.util.IdentityArraySet;
import org.checkerframework.org.plumelib.util.UnmodifiableIdentityHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A processor that stores the control flow graph for each method in the file.
 */
@SupportedAnnotationTypes("*")
public class FileProcessor extends BasicTypeProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);

    private CompilationUnitTree compilationUnitTree;

    private LineMap lineMap;

    final private Map<MethodTree, ControlFlowGraph> cfgResults;

    private final Map<MethodTree, Map<Node, Tree>> nodeMap;

    private final MethodScanner methodScanner;

    public FileProcessor() {
        cfgResults = new HashMap<>();
        nodeMap = new HashMap<>();
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

            // TODO: Move everything below out of this method.
            ControlFlowGraph cfg = CFGBuilder.build(compilationUnitTree, method, classTree, processingEnv);

            // TODO: Refactor to return the set of statements by overriding `reduce`.
            TreeScanner<Void, Set<Tree>> statementScanner = new StatementScanner(method);
            Set<Tree> statements = new IdentityArraySet<>();
            statementScanner.scan(method, statements);

            System.out.println(cfg.toStringDebug());
            System.out.println(cfg);
            System.out.println(method);
            System.out.println("Statements: " + statements.size());

            final UnmodifiableIdentityHashMap<UnaryTree, BinaryTree> postfixNodeLookup = cfg.getPostfixNodeLookup();

            // Using an equality hashmap instead of identity hashmap because we want that different cfg nodes represent the same node
            HashMap<Node, Tree> cfgNodesToPdgNodes = new HashMap<>();
            for (final Tree statement : statements) {
                Set<Node> found = new IdentityArraySet<>();
                TreeScanner<Void, Set<Node>> scanner = new CfgNodesScanner(cfg);
                scanner.scan(statement, found);

                final BinaryTree binaryTree = postfixNodeLookup.get(statement);
                if (binaryTree != null) {
                    scanner.scan(binaryTree, found);
                }
                found.forEach(node -> cfgNodesToPdgNodes.put(node, statement));
            }

            final Map<Tree, Set<Node>> collect = cfgNodesToPdgNodes.entrySet().stream().collect(
                    Collectors.groupingBy(
                            Map.Entry::getValue,
                            Collectors.mapping(Map.Entry::getKey, Collectors.toSet())
                    )
            );
            collect.forEach((k, v) -> System.out.println(k + " -> " + v));

            cfgResults.put(method, cfg);
            nodeMap.put(method, cfgNodesToPdgNodes);
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

    public Map<MethodTree, Map<Node, Tree>> getNodeMap() {
        return nodeMap;
    }

}
