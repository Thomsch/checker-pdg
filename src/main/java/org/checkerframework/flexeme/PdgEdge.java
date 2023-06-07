package org.checkerframework.flexeme;

import org.checkerframework.dataflow.cfg.node.Node;

public class PdgEdge {

    public final PdgNode from;
    public final PdgNode to;
    public final Type type;

    public PdgEdge(final PdgNode from, final PdgNode to, final Type type) {
        this.from = from;
        this.to = to;
        this.type = type;
    }


    enum Type {
        CONTROL(0, "black", "solid"), DATA(1, "darkseagreen4", "dashed"), CALL(2, "black", "dotted"), NAME(3, "darkorchid", "bold"), EXIT(0, "blue", "bold");

        private final int key;
        private final String color;
        private final String style;

        Type(int key, String color, String style) {
            this.key = key;
            this.color = color;
            this.style = style;
        }

        public String getStyle() {
            return style;
        }

        public String getColor() {
            return color;
        }

        public int getKey() {
            return key;
        }
    }
}
