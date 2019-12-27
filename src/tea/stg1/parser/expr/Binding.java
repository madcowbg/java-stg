package tea.stg1.parser.expr;

public class Binding {
    public final Variable variable;
    public final HeapObject valueExpr;

    public Binding(Variable variable, HeapObject valueExpr) {
        this.variable = variable;
        this.valueExpr = valueExpr;
    }

    @Override
    public String toString() {
        return "B<" + variable + ", " + valueExpr.toString() + ">";
    }

}
