package org.checkerframework.flexeme.nameflow;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents the result of the nameflow analysis to serialize to JSON.
 */
public class JsonResult {
    final private List<String> nodes;
    final private List<String> relations;

    public JsonResult() {
        this.nodes = new ArrayList<>();
        this.relations = new ArrayList<>();
    }

    public void addNode(final String variable) {
        nodes.add(variable);
    }

    public void addEdge(final String target, final Name source) {
        relations.add(String.format("%s -> %s", source, target));
    }
}
