package tea.stg1.parser.expr;

import java.util.List;
import java.util.stream.Collectors;

public class Func extends HeapObject implements Value {
    public final List<Variable> vars;
    public final Expr body;

    public Func(List<Variable> vars, Expr body) {
        this.vars = vars;
        this.body = body;
    }

    @Override
    public String toString() {
        return "FUN<" + vars.stream().map(Variable::toString).collect(Collectors.joining(" ", " ", "")) + " -> " + body.toString() + ">";
    }
}
