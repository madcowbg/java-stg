package tea.parser.expr;

import java.util.List;
import java.util.stream.Collectors;

public class FuncCall implements Expr {
    public List<Expr> exprs;
    public Expr f;

    public FuncCall(Expr f, List<Expr> exprs) {
        this.f = f;
        this.exprs = exprs;
    }

    @Override
    public String toString() {
        return "FCALL<" + f + ":" + exprs.stream().map(Expr::toString).collect(Collectors.joining(" ")) + ">";
    }
}
