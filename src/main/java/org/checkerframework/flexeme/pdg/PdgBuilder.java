package org.checkerframework.flexeme.pdg;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.builder.CFGBuilder;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.flexeme.*;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.org.plumelib.util.IdentityArraySet;
import org.checkerframework.org.plumelib.util.UnmodifiableIdentityHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ExecutableElement;
import java.util.*;

/**
 * Builds {@link org.checkerframework.flexeme.pdg.MethodPdg} and {@link org.checkerframework.flexeme.pdg.FilePdg}.
 * The input to the builder is the compilation results for a file as a {@link org.checkerframework.flexeme.FileProcessor}.
 * The output is a {@link org.checkerframework.flexeme.pdg.FilePdg} containing the PDGs for each method in the file or
 * a {@link org.checkerframework.flexeme.pdg.MethodPdg} for a single method.
 */
public class PdgBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PdgBuilder.class);

    /**
     * Build the PDGs for each method in the file.
     * @param processor The processor containing the compilation results for the file
     * @return A holder object for the PDGs for the file
     */
    public FilePdg buildPdgForFile(final FileProcessor processor) {
        // Build the PDG for each method in the compiled file.
        Set<MethodPdg> graphs = new HashSet<>();
        for (final MethodTree methodAst : processor.getMethodsAst()) {
            MethodPdg methodPdg = buildPdg(processor, methodAst);
            graphs.add(methodPdg);
        }

        Set<PdgEdge> localCalls = findLocalMethodCalls(graphs);

        return new FilePdg(graphs, localCalls);
    }

    /**
     * Build the PDG for a method using compilation results.
     * @param processor The processor with the compilation results.
     * @param methodAst The method AST to build the PDG for.
     * @return The PDG for the method.
     */
    public MethodPdg buildPdg(final FileProcessor processor, final MethodTree methodAst) {
        final ControlFlowGraph methodCfg = processor.getMethodCfg(methodAst);
        final ClassTree classTree = processor.getClassTree(methodAst);

        final Set<Tree> pdgElements = retrievePdgElements(methodAst);
        final Map<Node, Tree> cfgNodesToPdgElements = buildCfgNodeToPdgElementMap(pdgElements, methodCfg);

        // System.out.println(methodCfg.toStringDebug());
        // System.out.println();
        // System.out.println("PDG Elements: " + pdgElements.size());
        // for (final Tree statement : pdgElements) {
        //     System.out.println("    " + statement);
        // }

        // Show all the nodes associated with a statement.
        // The map is reversed so that the statement is the key.
        // System.out.println("PDG Elements -> Nodes");
        // final Map<Tree, Set<Node>> collect = cfgNodesToPdgElements.entrySet().stream().collect(
        //         Collectors.groupingBy(
        //                 Map.Entry::getValue,
        //                 Collectors.mapping(Map.Entry::getKey, Collectors.toSet())
        //         )
        // );
        // collect.forEach((k, v) -> System.out.println(k + " -> " + v));
        // System.out.println();

        // cfgResults.put(methodAst, methodCfg);
        // cfgNodesToPdgElementsMaps.put(methodAst, cfgNodesToPdgElements);


        // Add the nodes to the PDG.
        MethodPdg methodPdg = new MethodPdg(processor, classTree, methodAst, methodCfg, cfgNodesToPdgElements);
        for (Tree pdgElement : pdgElements) {
            methodPdg.addNode(pdgElement);
        }

        // Extract CFG edges and convert them to PDG edges.
        methodPdg.registerSpecialBlock(methodCfg.getEntryBlock(), "Entry");
        methodPdg.registerSpecialBlock(methodCfg.getRegularExitBlock(), "Exit");
        methodPdg.registerSpecialBlock(methodCfg.getExceptionalExitBlock(), "ExceptionalExit");
        CfgTraverser cfgTraverser = new CfgTraverser(null, cfgNodesToPdgElements, methodCfg);
        cfgTraverser.traverseEdges(methodPdg, methodCfg);

        addDataFlowEdges(methodPdg, processor, methodAst);

        return methodPdg;
    }

    private Map<Node, Tree> buildCfgNodeToPdgElementMap(final Set<Tree> pdgElements, final ControlFlowGraph cfg) {
        final UnmodifiableIdentityHashMap<UnaryTree, BinaryTree> postfixNodeLookup = cfg.getPostfixNodeLookup();

        // An identity hashmap is needed so that the nodes are compared by reference instead of equality.
        Map<Node, Tree> cfgNodesToPdgElements = new IdentityHashMap<>();
        for (final Tree pdgElement : pdgElements) {
            Set<Node> found = new IdentityArraySet<>();
            TreeScanner<Void, Set<Node>> scanner = new CfgNodesScanner(cfg);
            scanner.scan(pdgElement, found);

            // Retrieve postfix operations.
            final BinaryTree binaryTree = postfixNodeLookup.get(pdgElement);
            if (binaryTree != null) {
                scanner.scan(binaryTree, found);
            }

            // for (final Node node : found) {
            //     System.out.println("Found node: " + node + " (uid:" + node.getUid() + ") -> " + pdgElement);
            // }
            found.forEach(cfgNode -> cfgNodesToPdgElements.put(cfgNode, pdgElement));
        }
        return cfgNodesToPdgElements;
    }

    private Set<Tree> retrievePdgElements(final MethodTree methodAst) {
        // TODO: Refactor to return the set of pdgElements by overriding `reduce`.
        TreeScanner<Void, Set<Tree>> pdgElementScanner = new PdgElementScanner(methodAst);
        Set<Tree> pdgElements = new IdentityArraySet<>();
        pdgElementScanner.scan(methodAst, pdgElements);
        return pdgElements;
    }

    /**
     * Find the local method calls between methods in the file.
     * @param graphs The PDGs for the methods in the file
     * @return The set of local method calls between methods in the file
     */
    private Set<PdgEdge> findLocalMethodCalls(final Set<MethodPdg> graphs) {
        Set<PdgEdge> localCalls = new HashSet<>();
        // Build the method calls between files.
        HashMap<String, MethodPdg> methodNames = new HashMap<>();
        // Register local method invocations.
        for (final MethodPdg pdg : graphs) {
            final ExecutableElement executableElement = TreeUtils.elementFromDeclaration(pdg.getTree());
            String methodName = ElementUtils.getQualifiedName(executableElement);
            methodNames.put(methodName, pdg);
        }


        for (MethodPdg methodPdg : graphs) {
            for (Tree pdgElement : methodPdg.getPdgElements()) {
                TreeScanner<Set<ExecutableElement>, Void> c = new LocalMethodCallVisitor();
                Set<ExecutableElement> methodCalls = c.scan(pdgElement, null);

                if (methodCalls == null) {
                    continue;
                }

                for (final ExecutableElement methodCall : methodCalls) {
                    String methodName = ElementUtils.getQualifiedName(methodCall);
                    if (methodNames.containsKey(methodName)) {
                        PdgNode from = methodPdg.getNode(pdgElement);
                        final MethodPdg targetPdg = methodNames.get(methodName);
                        PdgNode to = targetPdg.getStartNode();
                        final PdgEdge edge = new PdgEdge(from, to, PdgEdge.Type.CALL);
                        localCalls.add(edge);
                    }
                }
            }
        }
        return localCalls;
    }

    private void addDataFlowEdges(final MethodPdg methodPdg, final FileProcessor processor, final MethodTree methodTree) {
        // // TODO: The creation of the graph should be decoupled from printing the results
        // // 3. Run nameflow analysis and add it to the graph.
        // processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
        //     ForwardAnalysis<NameRecord, NameFlowStore, NameFlowTransfer> analysis = new ForwardAnalysisImpl<>(new NameFlowTransfer());
        //     analysis.performAnalysis(controlFlowGraph);
        //
        //     final NameFlowStore exitStore = analysis.getRegularExitStore() == null ? analysis.getExceptionalExitStore() : analysis.getRegularExitStore();
        //     exitStore.getXi().forEach((variable, names) -> {
        //         names.forEach(nameRecord -> {
        //             // Certain nameflow edges have no corresponding nodes in the PDG (e.g., parameter bindings) so we ignore them.
        //             if (CfgTraverser.getNodes().contains(nameRecord.getUid()) && CfgTraverser.getNodes().contains(variable)) {
        //                 graphs.append(nameRecord.getUid()).append(" -> ").append(variable).append(" [key=3, style=bold, color=darkorchid]").append(System.lineSeparator());
        //             }
        //         });
        //     });
        // });
        //
        // graphs.append("}");
    }

    /**
     * Create the CFG with dataflow edges for a given method.
     *
     * @param analysis               The results of the dataflow analysis
     * @param methodControlFlowGraph The CFG of the method to visualize
     * @param methodTree
     * @return The visualizer object containing the PDG for the method.
     */
    // private static CfgTraverser runVisualization(ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis, ControlFlowGraph methodControlFlowGraph, FileProcessor processor, final MethodTree methodTree) {
    //     Map<String, Object> args = new HashMap<>(2);
    //     args.put("outdir", "out");
    //     args.put("verbose", true);
    //
    //     UnderlyingAST underlyingAST = methodControlFlowGraph.getUnderlyingAST();
    //     UnderlyingAST.CFGMethod method1 = ((UnderlyingAST.CFGMethod) underlyingAST);
    //
    //     CfgTraverser viz = new CfgTraverser(null, processor.getCfgNodeToPdgElementMaps().get(methodTree), processor.getMethodCfgs().get(methodTree));
    //     viz.init(args);
    //     Map<String, Object> res = viz.visualize(methodControlFlowGraph, methodControlFlowGraph.getEntryBlock(), analysis);
    //     viz.shutdown();
    //     return viz;
    // }

    /**
     * Runs the dataflow analysis for a given method.
     * @param methodControlFlowGraph The CFG of the method to analyze
     * @return The spent dataflow analysis.
     */
    private static ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> runAnalysis(ControlFlowGraph methodControlFlowGraph) {
        ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis = new ForwardAnalysisImpl<>(new DataflowTransfer());
        analysis.performAnalysis(methodControlFlowGraph);
        return analysis;
    }
}
