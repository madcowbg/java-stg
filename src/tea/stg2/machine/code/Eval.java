package tea.stg2.machine.code;

import tea.stg2.parser.expr.Expr;
import tea.stg2.machine.state.Environment;

public class Eval implements Code {
    public final Expr e;
    public final Environment localEnv;

    public Eval(Expr e, Environment localEnv) {
        this.e = e;
        this.localEnv = localEnv;
    }

    @Override
    public String toString() {
        return "Eval (" + e + ") (" + localEnv.toStr(";") + ")";
    }
}
