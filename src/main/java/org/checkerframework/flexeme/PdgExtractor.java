package org.checkerframework.flexeme;

import com.sun.source.tree.LineMap;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import org.checkerframework.checker.codechanges.FlexemeDataflowStore;
import org.checkerframework.checker.codechanges.FlexemeDataflowTransfer;
import org.checkerframework.checker.codechanges.FlexemeDataflowValue;
import org.checkerframework.checker.codechanges.FlexemePDGVisualizer;
import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.javacutil.UserError;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PdgExtractor {
    public static void main(String[] args) throws Throwable {
//        0. Setting environment / parsing arguments
        String file = "src/test/resources/BasicTests.java";
        String compile_out = "out/";
        String path_out = "pdg.dot"; // Where to write the results

//        1. Compile file. in: file path. out: cfgs
        FileProcessor processor = compileFile(file, compile_out, false); // Returns the spent processor with the compilation results.

//        2. Run analysis for each method. in: cfg, out: analysis done
        StringBuilder graphs = new StringBuilder("digraph {");
        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            System.out.println("Processing " + methodTree.getName().toString());
            ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis = runAnalysis(controlFlowGraph);
            String graph = runVisualization(analysis, controlFlowGraph, processor.getLineMap());
            graphs.append(graph);
        });

        FlexemePDGVisualizer.invocations.forEach((nodeId, methodName) -> {
            String blockId = FlexemePDGVisualizer.methods.get(methodName);
            if (blockId != null) {
                System.out.println("Adding call for " + methodName + ": " + nodeId + " -> " + blockId);
                graphs.append(nodeId).append(" -> ").append(blockId).append(" [key=2, style=dotted]");
            }
        });

        graphs.append("}");

//        3. Stitch results together.
// In the visualizer

//        4. Print dot file.
        try (BufferedWriter out = new BufferedWriter(new FileWriter("pdg.dot"))) {
            out.write(graphs.toString());
        } catch (IOException e) {
            throw new UserError("Error creating dot file (is the path valid?): all.dot", e);
        }

    }

    private static String runVisualization(ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis, ControlFlowGraph methodControlFlowGraph, LineMap lineMap) {
        Map<String, Object> args = new HashMap<>(2);
        args.put("outdir", "out");
        args.put("verbose", true);

        UnderlyingAST underlyingAST = methodControlFlowGraph.getUnderlyingAST();
        UnderlyingAST.CFGMethod method1 = ((UnderlyingAST.CFGMethod) underlyingAST);

        String cluster = makeClusterLabel(null, method1.getSimpleClassName(), method1.getMethodName(), method1.getMethod().getParameters());
        FlexemePDGVisualizer viz = new FlexemePDGVisualizer(cluster, lineMap, null);
        viz.init(args);
        Map<String, Object> res = viz.visualize(methodControlFlowGraph, methodControlFlowGraph.getEntryBlock(), analysis);
        viz.shutdown();
        return viz.getGraph();
    }

    private static String makeClusterLabel(String packageName, String className, String methodName, java.util.List<? extends VariableTree> parameters) {
        StringJoiner sjParameters = new StringJoiner(",");

        for (VariableTree parameter : parameters) {
            sjParameters.add(parameter.getType().toString());
        }

        return packageName + "." + className + "." + methodName + "(" + sjParameters + ")";
    }

    private static ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> runAnalysis(ControlFlowGraph methodControlFlowGraph) {
        ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis = new ForwardAnalysisImpl<>(new FlexemeDataflowTransfer());
        analysis.performAnalysis(methodControlFlowGraph);

        return analysis;
    }

    private static FileProcessor compileFile(String filepath, String compile_out, boolean compile_verbose) {
        java.util.List<String> arguments = new ArrayList<>();
        arguments.add("-d");
        arguments.add(compile_out);

        if (compile_verbose){
            arguments.add("-verbose");
        }

        arguments.add("-sourcepath");
        arguments.add("hello");

        Context context = new Context();
        JavacFileManager.preRegister(context); // Necessary to have fileManager before javac.
        JavacFileManager fileManager = (JavacFileManager) context.get(JavaFileManager.class);
        Iterable<? extends JavaFileObject> jFile = fileManager.getJavaFileObjectsFromStrings(List.of(filepath));

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        JavaCompiler.CompilationTask task = javac.getTask(null, null, null, arguments, null, jFile);
        FileProcessor o = new FileProcessor();
        task.setProcessors(Collections.singleton(o));
        task.call();
        return o;
    }
}
