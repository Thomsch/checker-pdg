package org.checkerframework.flexeme;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
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
import org.checkerframework.flexeme.pdg.FilePdg;
import org.checkerframework.flexeme.pdg.MethodPdg;
import org.checkerframework.flexeme.pdg.PdgEdge;
import org.checkerframework.flexeme.pdg.PdgNode;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.UserError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.ExecutableElement;
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
 * Extracts a Program Dependency Graph (PDG) from a Java file using the CheckerFramework.
 * The PDG is generated by compiling the file and running a dataflow analysis on each method.
 * The PDG is built to Flexeme PDG's format.
 */
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
        } catch (Throwable e) {
            logger.error("Error while running the PDG extractor: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public void run(String file, String sourcePath, String classPath, String path_out) {
        // Compile file and build CFGs.
        FileProcessor processor = compileFile(file, compileOut, false, sourcePath, classPath);

        // Build the PDG for each method in the compiled file.
        FilePdg filePdg = buildPdgs(processor);

        // Print the PDGs as a dot graph on the console
        DotPrinter printer = new DotPrinter();
        String dotGraphForFile = printer.printDot(filePdg);
        System.out.println(dotGraphForFile);

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

        writePdgOnDisk(dotGraphForFile, path_out);
    }

    /**
     * Build the PDGs for each method in the file.
     * @param processor The processor containing the compilation results for the file
     * @return A holder object for the PDGs for the file
     */
    public FilePdg buildPdgs(final FileProcessor processor) {

        // Build the PDG for each method in the compiled file.
        Set<MethodPdg> graphs = new HashSet<>();
        for (final MethodTree methodTree : processor.getMethods()) {
            MethodPdg methodPdg = buildPdg(processor, methodTree);
            graphs.add(methodPdg);
        }

        // Build the method calls between files.
        HashMap<String, MethodPdg> methodNames = new HashMap<>();
        // Register local method invocations.
        for (final MethodPdg pdg : graphs) {
            final ExecutableElement executableElement = TreeUtils.elementFromDeclaration(pdg.getTree());
            String methodName = ElementUtils.getQualifiedName(executableElement);
            methodNames.put(methodName, pdg);
        }

        Set<PdgEdge> localCalls = new HashSet<>();
        for (MethodPdg methodPdg : graphs) {
            for (Tree pdgElement : processor.getPdgElements(methodPdg.getTree())) {
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

        return new FilePdg(graphs, localCalls);
    }

    /**
     * Write the PDG to disk.
     *
     * @param pdg A string representation of the PDG to write on disk.
     * @param path_out The path where to write the PDG.
     */
    private void writePdgOnDisk(final String pdg, final String path_out) {
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path_out))) {
            out.write(pdg);
        } catch (IOException e) {
            throw new UserError("Error creating dot file (is the path valid?): all.dot", e);
        }
    }

    /**
     * Build the PDG for a method using compilation results.
     * @param processor The processor with the compilation results.
     * @param methodTree The method to build the PDG for.
     * @return The PDG for the method.
     */
    public MethodPdg buildPdg(final FileProcessor processor, final MethodTree methodTree) {
        MethodPdg methodPdg = new MethodPdg(processor, processor.getClassTree(methodTree), methodTree);
        for (Tree node : processor.getPdgElements(methodTree)) {
            methodPdg.addNode(node);
        }

        // Extract CFG edges and convert them to PDG edges.
        final ControlFlowGraph controlFlowGraph = processor.getMethodCfgs().get(methodTree);
        methodPdg.registerSpecialBlock(controlFlowGraph.getEntryBlock(), "Entry");
        methodPdg.registerSpecialBlock(controlFlowGraph.getRegularExitBlock(), "Exit");
        methodPdg.registerSpecialBlock(controlFlowGraph.getExceptionalExitBlock(), "ExceptionalExit");
        CfgTraverser cfgTraverser = new CfgTraverser(null, processor.getCfgNodeToPdgElementMaps().get(methodTree), processor.getMethodCfgs().get(methodTree));
        cfgTraverser.traverseEdges(methodPdg, controlFlowGraph);

        return methodPdg;
    }

    /**
     * Create the CFG with dataflow edges for a given method.
     *
     * @param analysis               The results of the dataflow analysis
     * @param methodControlFlowGraph The CFG of the method to visualize
     * @param methodTree
     * @return The visualizer object containing the PDG for the method.
     */
    private static CfgTraverser runVisualization(ForwardAnalysis<VariableReference, DataflowStore, DataflowTransfer> analysis, ControlFlowGraph methodControlFlowGraph, FileProcessor processor, final MethodTree methodTree) {
        Map<String, Object> args = new HashMap<>(2);
        args.put("outdir", "out");
        args.put("verbose", true);

        UnderlyingAST underlyingAST = methodControlFlowGraph.getUnderlyingAST();
        UnderlyingAST.CFGMethod method1 = ((UnderlyingAST.CFGMethod) underlyingAST);

        CfgTraverser viz = new CfgTraverser(null, processor.getCfgNodeToPdgElementMaps().get(methodTree), processor.getMethodCfgs().get(methodTree));
        viz.init(args);
        Map<String, Object> res = viz.visualize(methodControlFlowGraph, methodControlFlowGraph.getEntryBlock(), analysis);
        viz.shutdown();
        return viz;
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
    public FileProcessor compileFile(String filepath, String compile_out, boolean compile_verbose, String sourcePath, String classPath) {
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
