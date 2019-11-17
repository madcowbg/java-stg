package tea.stg2.parser;

import tea.stg2.parser.expr.Variable;

public class Binds {
    public final Variable var;
    public final LambdaForm lf;

    public Binds(Variable var, LambdaForm lf) {
        this.var = var;
        this.lf = lf;
    }

    @Override
    public String toString() {
        return var.name + " = " + lf + "";
    }
}
