package tea.stg2.parser;

import tea.stg2.parser.expr.Expr;
import tea.stg2.parser.expr.Variable;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LambdaForm {
    public final Variable[] freeVars;
    public final boolean pi;
    public final Variable[] boundVars;
    public final Expr expr;

    public LambdaForm(Variable[] freeVars, boolean pi, Variable[] boundVars, Expr expr) {
        this.freeVars = freeVars;
        this.pi = pi;
        this.boundVars = boundVars;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return dump(freeVars, v -> v.name) + " " + (pi ? "\\u" : "\\n") + " " + dump(boundVars, v -> v.name) + " -> " + expr.toString();
    }

    private static <T> String dump(T[] freeVars, Function<T, String> printf) {
        return Arrays.stream(freeVars).map(printf).collect(Collectors.joining(", ", "{", "}"));
    }
}