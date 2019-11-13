package tea.parserStg2;

public class PrimOp implements Expr {
    public final String op;
    public final Atom[] args;

    public PrimOp(String op, Atom[] args) {
        this.op = op;
        this.args = args;
    }
}
