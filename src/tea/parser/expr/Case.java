package tea.parser.expr;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Case implements Expr {
    public final Expr expr;
    public final List<Alternative> alts;

    public Case(Expr expr, List<Alternative> alts) {
        this.expr = expr;
        this.alts = alts;
    }

    @Override
    public String toString() {
        return "case <" + expr + ", " + alts.stream().map(Alternative::toString).collect(Collectors.joining(";", "Alts<", ">")) + ">";
    }
}
