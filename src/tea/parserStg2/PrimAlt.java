package tea.parserStg2;

import tea.parserStg2.machine.Data;
import tea.parserStg2.machine.Int;
import tea.parserStg2.machine.Value;

import java.util.Arrays;

public class PrimAlt implements Alt {
    public final Literal lit;
    public final Expr expr;

    public PrimAlt(Literal lit, Expr expr) {

        this.lit = lit;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return lit + " -> " + expr;
    }

    @Override
    public boolean matchCon(Data c, Value[] xs) {
        throw new RuntimeExecutionFailed("can't use primitive alternative with data constructor: " + c + Arrays.toString(xs));
    }

    @Override
    public boolean matchPrim(Value k) {
        assert k instanceof Int;
        return lit.value == ((Int) k).value;
    }
}
