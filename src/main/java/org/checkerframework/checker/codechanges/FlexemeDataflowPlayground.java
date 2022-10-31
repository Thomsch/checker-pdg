package org.checkerframework.checker.codechanges;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.*;
import org.checkerframework.dataflow.cfg.CFGProcessor;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.cfg.visualize.DOTCFGVisualizer;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class FlexemeDataflowPlayground {

    public static void main(String[] args) {
        /* Configuration: change as appropriate */
        String inputFile = "tests/codechanges/Test.java"; // input file name and path
        String outputDir = "build/tmp"; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        // Run the analysis and create a PDF file
        FlexemeDataflowTransfer transfer = new FlexemeDataflowTransfer();
        ForwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> forwardAnalysis = new ForwardAnalysisImpl<>(transfer);

        visualize(inputFile, outputDir, method, clazz, true, false, forwardAnalysis);

        System.out.println(forwardAnalysis.getRegularExitStore());
    }

    /**
     * Visualizes the PDG in Flexeme's format
     * Copied from {@link CFGVisualizeLauncher} because it only supports {@link DOTCFGVisualizer}.
     */
    public static void visualize(String inputFile,
                                 String outputDir,
                                 String method,
                                 String clas,
                                 boolean pdf,
                                 boolean verbose,
                                 @Nullable Analysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> analysis) {
        ControlFlowGraph cfg = generateMethodCFG(inputFile, clas, method);
        if (analysis != null) {
            analysis.performAnalysis(cfg);
        }

        Map<String, Object> args = new HashMap<>(2);
        args.put("outdir", outputDir);
        args.put("verbose", verbose);

        CFGVisualizer<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> viz = new FlexemePDGVisualizer();
        viz.init(args);
        Map<String, Object> res = viz.visualize(cfg, cfg.getEntryBlock(), analysis);
        viz.shutdown();

        if (pdf && res != null) {
            assert res.get("dotFileName") != null : "@AssumeAssertion(nullness): specification";
            producePDF((String) res.get("dotFileName"));
        }
    }

    protected static void producePDF(String file) {
        try {
            String command = "dot -Tpdf \"" + file + "\" -o \"" + file + ".pdf\"";
            Process child = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", command});
            child.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected static ControlFlowGraph generateMethodCFG(String file, String clas, final String method) {

        CFGProcessor cfgProcessor = new CFGProcessor(clas, method);

        Context context = new Context();
        Options.instance(context).put("compilePolicy", "ATTR_ONLY");
        JavaCompiler javac = new JavaCompiler(context);

        JavaFileObject l;
        try (JavacFileManager fileManager = (JavacFileManager) context.get(JavaFileManager.class)) {
            l = fileManager.getJavaFileObjectsFromStrings(List.of(file)).iterator().next();
        } catch (IOException e) {
            throw new Error(e);
        }

        PrintStream err = System.err;
        try {
            // Redirect syserr to nothing (and prevent the compiler from issuing
            // warnings about our exception).
            System.setErr(
                    new PrintStream(
                            // In JDK 11+, this can be just "OutputStream.nullOutputStream()".
                            new OutputStream() {
                                @Override
                                public void write(int b) throws IOException {}
                            }));
            javac.compile(List.of(l), List.of(clas), List.of(cfgProcessor), List.nil());
        } catch (Throwable e) {
            // ok
        } finally {
            System.setErr(err);
        }

        CFGProcessor.CFGProcessResult res = cfgProcessor.getCFGProcessResult();

        if (res == null) {
            printError("internal error in type processor! method typeProcessOver() doesn't get called.");
            System.exit(1);
        }

        if (!res.isSuccess()) {
            printError(res.getErrMsg());
            System.exit(1);
        }

        return res.getCFG();
    }

    /**
     * Print error message.
     *
     * @param string error message
     */
    protected static void printError(@Nullable String string) {
        System.err.println("ERROR: " + string);
    }
}
