package tea.stg2.parser.expr;

import tea.stg2.parser.expr.Atom;
import tea.stg2.parser.expr.Expr;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PrimOp implements Expr {
    public final String op;
    public final Atom[] args;

    public PrimOp(String op, Atom[] args) {
        this.op = op;
        this.args = args;
    }

    @Override
    public String toString() {
        return op + Arrays.stream(args).map(Atom::toString).collect(Collectors.joining(", ", "{", "}"));
    }
}
