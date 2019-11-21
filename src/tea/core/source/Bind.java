package tea.core.source;

public class Bind {
    public final Def var;
    public final Expr expr;

    public Bind(Def var, Expr expr) {

        this.var = var;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return var.toString() + " = " + expr.toString();
    }
}
