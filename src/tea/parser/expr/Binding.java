package tea.parser.expr;

public class Binding {
    private final Variable variable;
    private final HeapObject valueExpr;

    public Binding(Variable variable, HeapObject valueExpr) {
        this.variable = variable;
        this.valueExpr = valueExpr;
    }

    @Override
    public String toString() {
        return "B<" + variable + ", " + valueExpr.toString() + ">";
    }

}
