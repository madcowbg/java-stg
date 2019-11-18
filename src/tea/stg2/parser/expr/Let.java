package tea.stg2.parser.expr;

import tea.stg2.parser.Bind;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Let implements Expr {
    public final Bind[] binds;
    public final Expr expr;
    public final boolean isRec;

    public Let(boolean isRec, Bind[] binds, Expr expr) {
        this.isRec = isRec;
        this.binds = binds;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "LET {" + Arrays.stream(binds).map(Bind::toString).collect(Collectors.joining("; ")) + "} IN " + expr;
    }
}
