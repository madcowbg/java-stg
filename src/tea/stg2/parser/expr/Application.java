package tea.stg2.parser.expr;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Application implements Expr {
    public final Variable f;
    public final Atom[] args;

    public Application(Variable f, Atom[] args) {
        this.f = f;
        this.args = args;
    }

    @Override
    public String toString() {
        return f + " " + Arrays.stream(args).map(Atom::toString).collect(Collectors.joining(", ", "{", "}"));
    }
}
