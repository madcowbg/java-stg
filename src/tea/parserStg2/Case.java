package tea.parserStg2;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Case implements Expr {
    public final Expr expr;
    public final Alt[] alts;

    public Case(Expr expr, Alt[] alts) {
        this.expr = expr;
        this.alts = alts;
    }

    @Override
    public String toString() {
        return "CASE " + expr + " of " + Arrays.stream(alts).map(Object::toString).collect(Collectors.joining(";", "{", "}"));
    }
}
