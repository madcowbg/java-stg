package tea.stg1.parser.expr;

public class DefaultAlternative extends Alternative {
    public final Variable variable;
    public final Expr expr;

    public DefaultAlternative(Variable variable, Expr expr) {
        this.variable = variable;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "Def<" + variable + " -> " + expr + ">";
    }
}
