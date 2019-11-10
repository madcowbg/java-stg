package tea.parser.expr;

public class Let implements Expr {
    public final Variable variable;
    public final HeapObject obj;
    public final Expr expr;

    public Let(Variable variable, HeapObject obj, Expr expr) {
        this.variable = variable;
        this.obj = obj;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "LET<" + variable + "=" + obj + " in " + expr + ">";
    }
}
