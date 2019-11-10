package tea.parser.expr;

import tea.parser.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public class DeConstructorAlt extends Alternative {
    public final Token cons;
    public final List<Variable> vars;
    public final Expr expr;

    public DeConstructorAlt(Token cons, List<Variable> vars, Expr expr) {
        this.cons = cons;
        this.vars = vars;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "DC<" + cons + vars.stream().map(Variable::toString).collect(Collectors.joining(" ", " ", "")) + " -> " + expr + ">";
    }
}
