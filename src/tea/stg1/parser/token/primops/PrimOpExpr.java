package tea.stg1.parser.token.primops;

import tea.stg1.parser.expr.Expr;
import tea.stg1.parser.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public class PrimOpExpr implements Expr {
    public final Token op;
    public final List<Expr> args;

    public PrimOpExpr(Token op, List<Expr> args) {
        this.op = op;
        this.args = args;
    }

    public int arity() {
        return 2;
    }

    @Override
    public String toString() {
        return "Plus<" + args.stream().map(Object::toString).collect(Collectors.joining(" ")) + ">";
    }
}
