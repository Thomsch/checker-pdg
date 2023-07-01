package org.checkerframework.flexeme;

import com.google.common.collect.Sets;
import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
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

    private final Map<MethodTree, Map<Node, Tree>> cfgNodesToPdgElementsMaps;

    private final MethodScanner methodScanner;
    private EndPosTable endPosTable;

    public FileProcessor() {
        cfgResults = new HashMap<>();
        cfgNodesToPdgElementsMaps = new HashMap<>();
        methodScanner = new MethodScanner();
    }

    @Override
    protected TreePathScanner<?, ?> createTreePathScanner(CompilationUnitTree root) {
        compilationUnitTree = root;

        if (root instanceof JCTree.JCCompilationUnit) {
            JCTree.JCCompilationUnit jcCompilationUnit = (JCTree.JCCompilationUnit) root;
            endPosTable = jcCompilationUnit.endPositions;
        } else {
            logger.warn("CompilationUnitTree is not an instance of JCTree.JCCompilationUnit");
        }

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

            // Skip empty static constructors.
            if (method.getName().toString().equals("<init>") && method.getBody().getStatements().size() == 1) {
                continue;
            }

            // TODO: Move everything below out of this method.
            ControlFlowGraph cfg = CFGBuilder.build(compilationUnitTree, method, classTree, processingEnv);

            // TODO: Refactor to return the set of pdgElements by overriding `reduce`.
            TreeScanner<Void, Set<Tree>> pdgElementScanner = new PdgElementScanner(method);
            Set<Tree> pdgElements = new IdentityArraySet<>();
            pdgElementScanner.scan(method, pdgElements);

            System.out.println(cfg.toStringDebug());
            System.out.println();
            System.out.println("PDG Elements: " + pdgElements.size());
            for (final Tree statement : pdgElements) {
                System.out.println("    " + statement);
            }
            System.out.println();

            final UnmodifiableIdentityHashMap<UnaryTree, BinaryTree> postfixNodeLookup = cfg.getPostfixNodeLookup();

            // An identity hashmap is needed so that the nodes are compared by reference instead of equality.
            Map<Node, Tree> cfgNodesToPdgElements = new IdentityHashMap<>();
            for (final Tree pdgElement : pdgElements) {
                Set<Node> found = new IdentityArraySet<>();
                TreeScanner<Void, Set<Node>> scanner = new CfgNodesScanner(cfg);
                scanner.scan(pdgElement, found);

                final BinaryTree binaryTree = postfixNodeLookup.get(pdgElement);
                if (binaryTree != null) {
                    scanner.scan(binaryTree, found);
                }
                for (final Node node : found) {
                    System.out.println("Found node: " + node + " (uid:" + node.getUid() + ") -> " + pdgElement);
                }
                found.forEach(node -> cfgNodesToPdgElements.put(node, pdgElement));
            }
            System.out.println();

            // Show all the nodes associated with a statement.
            // The map is reversed so that the statement is the key.
            System.out.println("PDG Elements -> Nodes");
            final Map<Tree, Set<Node>> collect = cfgNodesToPdgElements.entrySet().stream().collect(
                    Collectors.groupingBy(
                            Map.Entry::getValue,
                            Collectors.mapping(Map.Entry::getKey, Collectors.toSet())
                    )
            );
            collect.forEach((k, v) -> System.out.println(k + " -> " + v));
            System.out.println();

            cfgResults.put(method, cfg);
            cfgNodesToPdgElementsMaps.put(method, cfgNodesToPdgElements);
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

    public EndPosTable getEndPosTable() {
        return endPosTable;
    }

    public Map<MethodTree, Map<Node, Tree>> getCfgNodeToPdgElementMaps() {
        return cfgNodesToPdgElementsMaps;
    }

    public Set<Tree> getPdgElements(MethodTree methodTree) {
        return Sets.newHashSet(cfgNodesToPdgElementsMaps.get(methodTree).values());
    }

    public ClassTree getClassTree(final MethodTree methodTree) {
        return methodScanner.getClassMap().get(methodTree);
    }

    public Set<MethodTree> getMethods() {
        return cfgResults.keySet();
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
        for (final MethodTree methodTree : cfgResults.keySet()) {
            System.out.println("Method: " + methodTree.getName());
            if (methodTree.getName().toString().equals(methodName)) {
                return methodTree;
            }
        }
        throw new NoSuchElementException("No method with name " + methodName + " exists.");
    }
}
