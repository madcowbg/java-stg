package tea.parser.expr;

import tea.parser.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public class DeConstructor extends Alternative {
    private final Token cons;
    private final List<Variable> vars;
    private final Expr expr;

    public DeConstructor(Token cons, List<Variable> vars, Expr expr) {
        this.cons = cons;
        this.vars = vars;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "DC<" + cons + vars.stream().map(Variable::toString).collect(Collectors.joining(" ", " ", "")) + " -> " + expr + ">";
    }
}
