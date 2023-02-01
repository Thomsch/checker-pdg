package org.checkerframework.flexeme;

import com.sun.source.tree.LineMap;
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
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PdgExtractor {
    public static void main(String[] args) throws Throwable {
//        0. Setting environment / parsing arguments
        String file = "src/test/resources/BasicTests.java";
        String compile_out = "out/";
        String path_out = "pdg.dot"; // Where to write the results

//        1. Compile file. in: file path. out: cfgs
        FileProcessor processor = compileFile(file, compile_out, false); // Returns the spent processor with the compilation results.

//        2. Run analysis for each method. in: cfg, out: analysis done

        processor.getMethodCfgs().forEach((methodTree, controlFlowGraph) -> {
            System.out.println("Processing " + methodTree.getName().toString());
            ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis = runAnalysis(controlFlowGraph);
            runVisualization(analysis, controlFlowGraph, processor.getLineMap());
        });

//        3. Stitch results together.

//        4. Print dot file.

    }

    private static void runVisualization(ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis, ControlFlowGraph methodControlFlowGraph, LineMap lineMap) {
        Map<String, Object> args = new HashMap<>(2);
        args.put("outdir", "out");
        args.put("verbose", true);

        CFGVisualizer<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> viz = new FlexemePDGVisualizer("dummy", lineMap, null);
        viz.init(args);
        Map<String, Object> res = viz.visualize(methodControlFlowGraph, methodControlFlowGraph.getEntryBlock(), analysis);
        viz.shutdown();
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
