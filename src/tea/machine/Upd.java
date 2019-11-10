package tea.machine;

import tea.parser.expr.Variable;

public class Upd implements Continuation {
    final Variable variable;

    public Upd(Variable variable) {
        this.variable = variable;
    }

    @Override
    public String toString() {
        return "Upd " + variable + " *";
    }
}
