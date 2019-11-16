package tea.parserStg2;

import tea.parserStg2.machine.Data;
import tea.parserStg2.machine.Value;

import java.util.Arrays;

public class AlgAlt implements Alt {
    public final Cons<Variable> cons;
    public final Expr expr;

    public AlgAlt(Cons<Variable> cons, Expr expr) {
        this.cons = cons;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return cons + " -> " + expr;
    }

    @Override
    public boolean matchCon(Data c, Value[] xs) {
        assert cons.args.length == xs.length;
        return cons.cons.equals(c.cons);
    }

    @Override
    public boolean matchPrim(Value k) {
        throw new RuntimeExecutionFailed("can't use algebraic alternative with primitive: " + k);
    }
}
