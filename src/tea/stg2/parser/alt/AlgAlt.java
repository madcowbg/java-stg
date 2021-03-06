package tea.stg2.parser.alt;

import tea.stg2.machine.state.Data;
import tea.stg2.machine.value.Value;
import tea.stg2.machine.RuntimeExecutionFailed;
import tea.stg2.parser.expr.Cons;
import tea.stg2.parser.expr.Expr;
import tea.stg2.parser.expr.Variable;

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
        if (cons.cons.equals(c.cons)) {
            assert cons.args.length == xs.length : "constructor arity is " + cons.args.length + " but requested with " + xs.length + " parameters!";
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean matchPrim(Value k) {
        throw new RuntimeExecutionFailed("can't use algebraic alternative with primitive: " + k);
    }
}
