package tea.parserStg2;

import tea.parserStg2.machine.Data;
import tea.parserStg2.machine.Value;

import java.util.Optional;

public class DefaultAlt implements Alt {

    public final Optional<Variable> var;
    public final Expr expr;

    public DefaultAlt(Optional<Variable> var, Expr expr) {
        this.var = var;
        this.expr = expr;
    }

    @Override
    public boolean matchCon(Data c, Value[] xs) {
        return true;
    }

    @Override
    public boolean matchPrim(Value k) {
        return true;
    }

    @Override
    public String toString() {
        return var.map(Variable::toString).orElse("default") + " -> " + expr;
    }
}
