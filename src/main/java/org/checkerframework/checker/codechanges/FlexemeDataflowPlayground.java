package org.checkerframework.checker.codechanges;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

public class FlexemeDataflowPlayground {

    public static void main(String[] args) {
        /* Configuration: change as appropriate */
        String inputFile = "tests/codechanges/Test.java"; // input file name and path
        String outputDir = "build/tmp"; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        // Run the analysis and create a PDF file
        FlexemeDataflowTransfer transfer = new FlexemeDataflowTransfer();
        BackwardAnalysis<FlexemeDataflowValue, FlexemeDataflowStore, FlexemeDataflowTransfer> backwardAnalysis = new BackwardAnalysisImpl<>(transfer);

        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        cfgVisualizeLauncher.generateDOTofCFG(inputFile, outputDir, method, clazz, true, true, backwardAnalysis);
    }
}
