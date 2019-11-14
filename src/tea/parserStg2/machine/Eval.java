package tea.parserStg2.machine;

import tea.parserStg2.Expr;

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
