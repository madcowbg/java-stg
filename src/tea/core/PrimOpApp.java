package tea.core;

import tea.core.source.Expr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class PrimOpApp implements Expr {
    public final String op;
    public final Expr[] args;

    public PrimOpApp(String op, Expr[] args) {
        this.op = op;
        this.args = args;
    }

    @Override
    public String toString() {
        return "(" + op + " " + Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" ")) + ")";
    }
}
