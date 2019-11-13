package tea.parserStg2;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LF {
    public final Identifier[] freeVars;
    public final boolean pi;
    public final Identifier[] boundVars;
    public final Expr expr;

    public LF(Identifier[] freeVars, boolean pi, Identifier[] boundVars, Expr expr) {
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
