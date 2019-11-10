package tea.machine.continuation;

import tea.parser.expr.Variable;

public class Upd implements Continuation {
    public final Variable variable;

    public Upd(Variable variable) {
        this.variable = variable;
    }

    @Override
    public String toString() {
        return "Upd " + variable + " *";
    }
}
