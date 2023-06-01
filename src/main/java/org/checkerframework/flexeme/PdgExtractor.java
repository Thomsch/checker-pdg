package org.checkerframework.flexeme;

import com.google.common.graph.MutableNetwork;
import com.google.common.graph.Network;
import com.google.common.graph.NetworkBuilder;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.flexeme.dataflow.DataflowStore;
import org.checkerframework.flexeme.dataflow.DataflowTransfer;
import org.checkerframework.flexeme.dataflow.VariableReference;
import org.checkerframework.flexeme.nameflow.NameFlowStore;
import org.checkerframework.flexeme.nameflow.NameFlowTransfer;
import org.checkerframework.flexeme.nameflow.NameRecord;
import org.checkerframework.javacutil.UserError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Extracts a flexeme-PDG from a Java file.
 * The PDG is generated by compiling the file and running a dataflow analysis on each method.
 * The dataflow analysis is done using the CheckerFramework.
 */
@SuppressWarnings("UnstableApiUsage")
public class PdgExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdgExtractor.class);
    private final String compileOut;

    public PdgExtractor() {
        compileOut = "out/";
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            logger.error("Usage: java PdgExtractor <file> <sourcePath> <classPath>");
            System.exit(1);
        }

        String file = args[0]; // Relative to the repository e.g., src/java/App.java
        String sourcePath = args[1];
        String classPath = args[2];
        String path_out = "pdg.dot"; // Where to write the PDG.

        PdgExtractor extractor = new PdgExtractor();
        try {
            extractor.run(file, sourcePath, classPath, path_out);
            // Compile file
            // Run analysis
            // Build graph
            // Print graph
        } catch (Throwable e) {
            logger.error("Error while running the PDG extractor: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public void run(String file, String sourcePath, String classPath, String path_out) {
        // 1. Compile file and dependencies.
        FileProcessor processor = compileFile(file, compileOut, false, sourcePath, classPath); // Returns the spent processor with the compilation results.

        // 2. Run analysis for each method and build the dataflow graph.
        // TODO: This is extremely tangled. The construction of the PDG and it's visualization are tangled. We should refactor this.
        StringBuilder graphs = new StringBuilder("digraph {");
        Set<PdgGraph> graphs2 = new HashSet<>();
        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            Set<Tree> pdgNodes = processor.getPdgNodes(methodTree);

            PdgGraph pdgGraph = new PdgGraph(processor, processor.getClassTree(methodTree), methodTree);

            for (Tree node : pdgNodes) {
                pdgGraph.addNode(node);
            }

            ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis = runAnalysis(controlFlowGraph);
            PDGVisualizer visualizer = runVisualization(analysis, controlFlowGraph, processor, methodTree);
            String graph = visualizer.getGraph();
            graphs.append(graph);
            graphs2.add(pdgGraph);
        });

        printDot(graphs2);

        // Method invocation edges.
        PDGVisualizer.invocations.forEach((nodeId, methodName) -> {
            String blockId = PDGVisualizer.methods.get(methodName);
            if (blockId != null) {
                graphs.append(nodeId).append(" -> ").append(blockId).append(" [key=2, style=dotted]").append(System.lineSeparator());
            }
        });

        // TODO: The creation of the graph should be decoupled from printing the results
        // 3. Run nameflow analysis and add it to the graph.
        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            ForwardAnalysis<NameRecord, NameFlowStore, NameFlowTransfer> analysis = new ForwardAnalysisImpl<>(new NameFlowTransfer());
            analysis.performAnalysis(controlFlowGraph);

            final NameFlowStore exitStore = analysis.getRegularExitStore() == null ? analysis.getExceptionalExitStore() : analysis.getRegularExitStore();
            exitStore.getXi().forEach((variable, names) -> {
                names.forEach(nameRecord -> {
                    // Certain nameflow edges have no corresponding nodes in the PDG (e.g., parameter bindings) so we ignore them.
                    if (PDGVisualizer.getNodes().contains(nameRecord.getUid()) && PDGVisualizer.getNodes().contains(variable)) {
                        graphs.append(nameRecord.getUid()).append(" -> ").append(variable).append(" [key=3, style=bold, color=darkorchid]").append(System.lineSeparator());
                    }
                });
            });
        });

        graphs.append("}");

        // 3. Print dot file.
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path_out))) {
            out.write(graphs.toString());
        } catch (IOException e) {
            throw new UserError("Error creating dot file (is the path valid?): all.dot", e);
        }
    }

    /**
     * Print PDGs graphs as one dot file.
     * @param graphs The PDGs graphs from a file to print.
     */
    private String printDot(Set<PdgGraph> graphs) {
        // StringBuilder dotGraph = new StringBuilder("digraph {");
        System.out.println("digraph {");
        int counter = 0;
        for (final PdgGraph graph : graphs) {
            printGraph(graph, counter);
            counter++;
        }

        // TODO: All the edges are printed at the end (that's what C# PDG does)
        System.out.println("}");
        return "";
    }

    private void printGraph(PdgGraph graph, int cluster) {
        System.out.println("subgraph " + "cluster_" + cluster + " {");
        printSubgraphLabel(graph);

        // Print nodes
        for (final PdgNode node : graph.nodes()) {
            printNode(node);
        }

        // Print edges
        // TODO

        System.out.println("}");
    }

    private void printNode(final PdgNode node) {
        System.out.printf("n%d [label=\"%s\", span=\"%d-%d\"];%n", node.getId(), node, node.getStartLine(), node.getEndLine());
    }

    private void printSubgraphLabel(final PdgGraph graph) {
        System.out.printf("label = \"%s.%s()\";%n", graph.getClassName(), graph.getMethodName());
    }

    /**
     * Create the CFG with dataflow edges for a given method.
     *
     * @param analysis               The results of the dataflow analysis
     * @param methodControlFlowGraph The CFG of the method to visualize
     * @param methodTree
     * @return The visualizer object containing the PDG for the method.
     */
    private static PDGVisualizer runVisualization(ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis, ControlFlowGraph methodControlFlowGraph, FileProcessor processor, final MethodTree methodTree) {
        Map<String, Object> args = new HashMap<>(2);
        args.put("outdir", "out");
        args.put("verbose", true);

        UnderlyingAST underlyingAST = methodControlFlowGraph.getUnderlyingAST();
        UnderlyingAST.CFGMethod method1 = ((UnderlyingAST.CFGMethod) underlyingAST);

        String cluster = makeClusterLabel(null, method1.getSimpleClassName(), method1.getMethodName(), method1.getMethod().getParameters());
        PDGVisualizer viz = new PDGVisualizer(cluster, processor.getLineMap(), null, processor.getNodeMap().get(methodTree), processor.getMethodCfgs().get(methodTree));
        viz.init(args);
        Map<String, Object> res = viz.visualize(methodControlFlowGraph, methodControlFlowGraph.getEntryBlock(), analysis);
        viz.shutdown();
        return viz;
    }

    /**
     * Creates the label for the cluster attribute in the dot file.
     */
    private static String makeClusterLabel(String packageName, String className, String methodName, java.util.List<? extends VariableTree> parameters) {
        StringJoiner sjParameters = new StringJoiner(",");

        for (VariableTree parameter : parameters) {
            sjParameters.add(parameter.getType().toString());
        }

        return packageName + "." + className + "." + methodName + "(" + sjParameters + ")";
    }

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

    /**
     * Compiles a file and returns the processor with the compilation results.
     * @param filepath path to the file to compile
     * @param compile_out where to put the compiled files
     * @param compile_verbose whether to print the compilation output
     * @param sourcePath source path for the compilation
     * @param classPath class path for the compilation
     * @return the processor with the compilation results
     */
    public static FileProcessor compileFile(String filepath, String compile_out, boolean compile_verbose, String sourcePath, String classPath) {
        java.util.List<String> arguments = new ArrayList<>();
        arguments.add("-d");
        arguments.add(compile_out);

        if (compile_verbose) {
            arguments.add("-verbose");
        }

        arguments.add("-sourcepath");
        arguments.add(sourcePath);

        arguments.add("-classpath");
        arguments.add(classPath);

        arguments.add("-source");
        arguments.add("1.8");
        arguments.add("-target");
        arguments.add("1.8");

        arguments.add("-Xlint:none"); // Ignore warnings

        Context context = new Context();
        JavacFileManager.preRegister(context); // Necessary to have fileManager before javac.
        JavacFileManager fileManager = (JavacFileManager) context.get(JavaFileManager.class);
        Iterable<? extends JavaFileObject> jFile = fileManager.getJavaFileObjectsFromStrings(List.of(filepath));

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

        StringWriter out = new StringWriter();

        JavaCompiler.CompilationTask task = javac.getTask(out, null, null, arguments, null, jFile);
        FileProcessor processor = new FileProcessor();
        task.setProcessors(Collections.singleton(processor));
        boolean result = task.call();

        if (!result) {
            throw new RuntimeException("Compilation failed for file: " + filepath, new Throwable(out.toString()));
        }

        return processor;
    }
}
