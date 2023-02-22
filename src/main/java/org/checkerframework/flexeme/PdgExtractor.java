package org.checkerframework.flexeme;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.source.tree.LineMap;
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
import org.checkerframework.flexeme.dataflow.DataflowValue;
import org.checkerframework.flexeme.nameflow.JsonResult;
import org.checkerframework.flexeme.nameflow.Name;
import org.checkerframework.flexeme.nameflow.NameFlowStore;
import org.checkerframework.flexeme.nameflow.NameFlowTransfer;
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
import java.io.Writer;
import java.util.*;

public class PdgExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdgExtractor.class);
    private final String compileOut;

    public static void main(String[] args) throws Throwable {
        String file = args[0]; // Relative to the repository e.g., src/java/App.java
        String sourcePath = args[1];
        String classPath = args[2];
        String path_out = "pdg.dot"; // Where to write the results TODO

        PdgExtractor extractor = new PdgExtractor();
        try {
            extractor.run(file, sourcePath, classPath, path_out);
        } catch (Throwable e) {
            logger.error("Error while running the PDG extractor: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    public PdgExtractor() {
        compileOut = "out/";
    }

    public void run(String file, String sourcePath, String classPath, String path_out) {
        // 1. Compile file. in: file path. out: cfgs
        FileProcessor processor = compileFile(file, compileOut, false, sourcePath, classPath); // Returns the spent processor with the compilation results.

        // 2. Run analysis for each method and build the graph. in: cfg, out: graph
        // TODO: This is extremely tangled. The construction of the PDG and it's visualization are tangled. We should refactor this.
        StringBuilder graphs = new StringBuilder("digraph {");
        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            ForwardAnalysis<DataflowValue, DataflowStore, DataflowTransfer> analysis = runAnalysis(controlFlowGraph);
            PDGVisualizer visualizer = runVisualization(analysis, controlFlowGraph, processor.getLineMap());
            String graph = visualizer.getGraph();
            graphs.append(graph);
        });

        PDGVisualizer.invocations.forEach((nodeId, methodName) -> {
            String blockId = PDGVisualizer.methods.get(methodName);
            if (blockId != null) {
                graphs.append(nodeId).append(" -> ").append(blockId).append(" [key=2, style=dotted]").append(System.lineSeparator());
            }
        });

        // TODO: The creation of the graph should be decoupled from printing the results
        // 1. Create graph in memory in a data structure.
        // 2. Print graph

        JsonResult result = new JsonResult();
        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            ForwardAnalysis<Name, NameFlowStore, NameFlowTransfer> analysis = new ForwardAnalysisImpl<>(new NameFlowTransfer());
            analysis.performAnalysis(controlFlowGraph);

            analysis.getRegularExitStore().getXi().forEach((variable, names) -> {
                result.addNode(variable);
                names.forEach(name -> {
                    result.addEdge(analysis.getRegularExitStore().names.get(variable) + "(" + variable + ")", name);

                    // Certain nameflow edges have no corresponding nodes in the PDG (e.g., parameter bindings) so we ignore them.
                    if (PDGVisualizer.getNodes().contains(name.getUid()) && PDGVisualizer.getNodes().contains(variable)) {
                        graphs.append(name.getUid()).append(" -> ").append(variable).append(" [key=3, style=bold, color=darkorchid]").append(System.lineSeparator());
                    }
                });
            });
            // System.out.println(analysis.getRegularExitStore());
        });

        // Write method name to json file.
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Save the results to a json file.
        gson.toJson(result, System.out);

        graphs.append("}");

        // 3. Print dot file.
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path_out))) {
            out.write(graphs.toString());
        } catch (IOException e) {
            throw new UserError("Error creating dot file (is the path valid?): all.dot", e);
        }


    }

    private static PDGVisualizer runVisualization(ForwardAnalysis<DataflowValue, DataflowStore, DataflowTransfer> analysis, ControlFlowGraph methodControlFlowGraph, LineMap lineMap) {
        Map<String, Object> args = new HashMap<>(2);
        args.put("outdir", "out");
        args.put("verbose", true);

        UnderlyingAST underlyingAST = methodControlFlowGraph.getUnderlyingAST();
        UnderlyingAST.CFGMethod method1 = ((UnderlyingAST.CFGMethod) underlyingAST);

        String cluster = makeClusterLabel(null, method1.getSimpleClassName(), method1.getMethodName(), method1.getMethod().getParameters());
        PDGVisualizer viz = new PDGVisualizer(cluster, lineMap, null);
        viz.init(args);
        Map<String, Object> res = viz.visualize(methodControlFlowGraph, methodControlFlowGraph.getEntryBlock(), analysis);
        viz.shutdown();
        return viz;
    }

    private static String makeClusterLabel(String packageName, String className, String methodName, java.util.List<? extends VariableTree> parameters) {
        StringJoiner sjParameters = new StringJoiner(",");

        for (VariableTree parameter : parameters) {
            sjParameters.add(parameter.getType().toString());
        }

        return packageName + "." + className + "." + methodName + "(" + sjParameters + ")";
    }

    private static ForwardAnalysis<DataflowValue, DataflowStore, DataflowTransfer> runAnalysis(ControlFlowGraph methodControlFlowGraph) {
        ForwardAnalysis<DataflowValue, DataflowStore, DataflowTransfer> analysis = new ForwardAnalysisImpl<>(new DataflowTransfer());
        analysis.performAnalysis(methodControlFlowGraph);
        return analysis;
    }

    /**
     * Compiles a file and returns the processor with the compilation results.
     * @param filepath Path to the file to compile
     * @param compile_out Where to put the compiled files
     * @param compile_verbose Whether to print the compilation output
     * @param sourcePath Source path for the compilation
     * @param classPath Class path for the compilation
     * @return The processor with the compilation results
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

        arguments.add("-Xlint:none"); // Ignore warnings

        Context context = new Context();
        JavacFileManager.preRegister(context); // Necessary to have fileManager before javac.
        JavacFileManager fileManager = (JavacFileManager) context.get(JavaFileManager.class);
        Iterable<? extends JavaFileObject> jFile = fileManager.getJavaFileObjectsFromStrings(List.of(filepath));

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

        NullOutputStream out = new NullOutputStream();

        JavaCompiler.CompilationTask task = javac.getTask(out, null, null, arguments, null, jFile);
        FileProcessor processor = new FileProcessor();
        task.setProcessors(Collections.singleton(processor));
        task.call();
        return processor;
    }

    /**
     * Prints the name flow analysis for a file.
     * @param inputFile The file to analyze.
     * @param compile_out The directory to compile the file to.
     */
    public static void nameFlow(final String inputFile, final String compile_out) {

        // Run compilation on file with the analysis.
        FileProcessor processor = compileFile(inputFile, compile_out, false, "", "");

        // Run analysis for each method.
        JsonResult result = new JsonResult();

        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            ForwardAnalysis<Name, NameFlowStore, NameFlowTransfer> analysis = new ForwardAnalysisImpl<>(new NameFlowTransfer());
            analysis.performAnalysis(controlFlowGraph);

            analysis.getRegularExitStore().getXi().forEach((variable, names) -> {
                result.addNode(variable);
                names.forEach(name -> {
                    result.addEdge(variable, name);
                });
            });
            // System.out.println(analysis.getRegularExitStore());
        });

        // Write method name to json file.
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Save the results to a json file.
        gson.toJson(result, System.out);
    }

    public static class NullOutputStream extends Writer {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {

        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public void close() throws IOException {

        }
    }
}
