package tea.parserStg2;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Let implements Expr {
    public final Binds[] binds;
    public final Expr expr;
    public final boolean isRec;

    public Let(boolean isRec, Binds[] binds, Expr expr) {
        this.isRec = isRec;
        this.binds = binds;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "LET {" + Arrays.stream(binds).map(Binds::toString).collect(Collectors.joining("; ")) + "} IN " + expr;
    }
}
