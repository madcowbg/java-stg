package tea.parser.expr;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class Case implements Expr {
    private final Expr expr;
    private final ArrayList<Alternative> alts;

    public Case(Expr expr, ArrayList<Alternative> alts) {
        this.expr = expr;
        this.alts = alts;
    }

    @Override
    public String toString() {
        return "case <" + expr + ", " + alts.stream().map(Alternative::toString).collect(Collectors.joining(";", "Alts<", ">")) + ">";
    }
}
