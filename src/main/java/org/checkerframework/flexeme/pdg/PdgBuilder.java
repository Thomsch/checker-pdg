package org.checkerframework.flexeme.pdg;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.flexeme.*;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.Edge;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.checkerframework.flexeme.nameflow.NameFlowStore;
import org.checkerframework.flexeme.nameflow.NameFlowTransfer;
import org.checkerframework.flexeme.nameflow.NameRecord;
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

        methodPdg.registerSpecialBlock(methodCfg.getEntryBlock(), "Entry");
        for (Tree pdgElement : pdgElements) {
            methodPdg.addNode(pdgElement);
        }
        methodPdg.registerSpecialBlock(methodCfg.getRegularExitBlock(), "Exit");
        methodPdg.registerSpecialBlock(methodCfg.getExceptionalExitBlock(), "ExceptionalExit");
        CfgTraverser cfgTraverser = new CfgTraverser();
        cfgTraverser.traverseEdges(methodPdg, methodCfg);

        addDataFlowEdges(methodPdg);

        addNameFlowEdges(methodPdg);

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

    /**
     * Add the data flow edges to the PDG.
     * @param methodPdg The PDG to add the edges to.
     */
    private void addDataFlowEdges(final MethodPdg methodPdg) {
        // This implementation reuses the legacy dataflow implementation with its {@link Edge} and {@link VariableReference}.
        // Ideally, the analysis would use the {@link PdgNode} and {@link PdgEdge} classes so there is no need to convert
        // between the two.
        final ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis = runAnalysis(methodPdg.getMethodCfg(), methodPdg.getPdgElements());
        Set<Edge> edges = new HashSet<>();
        if (analysis.getRegularExitStore() != null) {
            edges.addAll(analysis.getRegularExitStore().getEdges());
        }

        if (analysis.getExceptionalExitStore() != null) {
            edges.addAll(analysis.getExceptionalExitStore().getEdges());
        }

        for (final Edge edge : edges) {
            // If the from node is a parameter, the from PDG node is the Entry node.
            PdgNode from;
            if (methodPdg.getMethodAst().getParameters().contains(edge.getFrom().getReference().getTree())) {
                from = methodPdg.getStartNode();
            } else {
                // Otherwise, find the from node in the PDG.
                from = methodPdg.getNode(edge.getFrom().getReference());
            }
            PdgNode to = methodPdg.getNode(edge.getTo().getReference());

            if (from != null && to != null && edge.getFrom().getReference().getInSource() && edge.getTo().getReference().getInSource()) {
                PdgEdge pdgEdge = new PdgEdge(from, to, PdgEdge.Type.DATA);
                methodPdg.addEdge(pdgEdge);
            }
        }
    }

    /**
     * Run the name flow analysis.
     * @param methodPdg The PDG to run the analysis on.
     */
    private void addNameFlowEdges(final MethodPdg methodPdg) {
        ControlFlowGraph controlFlowGraph = methodPdg.getMethodCfg();

        // Perform the nameflow analysis.
        ForwardAnalysis<NameRecord, NameFlowStore, NameFlowTransfer> analysis = new ForwardAnalysisImpl<>(new NameFlowTransfer());
        analysis.performAnalysis(controlFlowGraph);

        // Convert the name flow analysis results to PDG edges.
        final Set<PdgEdge> edges = new HashSet<>();
        if (analysis.getRegularExitStore() != null) {
            edges.addAll(convertNameFlowStoreToPdgEdges(methodPdg, analysis.getRegularExitStore()));
        }
        if (analysis.getExceptionalExitStore() != null) {
            edges.addAll(convertNameFlowStoreToPdgEdges(methodPdg, analysis.getExceptionalExitStore()));
        }

        // Add the PDG edges to the PDG.
        for (final PdgEdge edge : edges) {
            methodPdg.addEdge(edge);
        }
    }

    /**
     * Converts the name flow store to a set of PDG edges.
     * @param methodPdg The PDG where the edges will be added.
     * @param store The name flow store to convert.
     * @return The set of PDG edges to add to the method PDG.
     */
    private Set<PdgEdge> convertNameFlowStoreToPdgEdges(final MethodPdg methodPdg, final NameFlowStore store) {
        Set<PdgEdge> edges = new HashSet<>();
        PdgNode entryNode = methodPdg.getStartNode();
        store.getReturnedVariables().forEach((name, node) -> {
            Node declarationNode = store.getVariableNode(name);
            PdgNode to = methodPdg.getNode(declarationNode);
            if (to != null) {
                PdgEdge pdgEdge = new PdgEdge(entryNode, to, PdgEdge.Type.NAME);
                methodPdg.addEdge(pdgEdge);
            }
        });

        store.getXi().forEach((variable, names) -> {
            PdgNode from = methodPdg.getNode(variable);
            names.forEach(nameRecord -> {
                if (nameRecord.getName().equals(variable.toString())) {
                    return;
                }

                if (nameRecord.isMethod()) {
                    return;
                }

                Node node = store.getVariableNode(nameRecord.getName());
                if (node != null) {
                    PdgNode to = methodPdg.getNode(node);
                    if (from != null && to != null) {
                        PdgEdge pdgEdge = new PdgEdge(from, to, PdgEdge.Type.NAME);
                        methodPdg.addEdge(pdgEdge);
                    }
                }
            });
        });
        return edges;
    }

    /**
     * Runs the dataflow analysis for a given method.
     *
     * @param methodControlFlowGraph The CFG of the method to analyze
     * @param pdgElements
     * @return The spent dataflow analysis.
     */
    private ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> runAnalysis(ControlFlowGraph methodControlFlowGraph, final Set<Tree> pdgElements) {
        ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis = new ForwardAnalysisImpl<>(new DataflowTransfer());
        analysis.performAnalysis(methodControlFlowGraph);
        return analysis;
    }
}
