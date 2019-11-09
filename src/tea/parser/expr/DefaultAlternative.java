package tea.parser.expr;

public class DefaultAlternative extends Alternative {
    private final Variable variable;
    private final Expr expr;

    public DefaultAlternative(Variable variable, Expr expr) {
        this.variable = variable;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "Def<" + variable + " -> " + expr + ">";
    }
}
