package tea.parserStg2;

import java.util.Arrays;
import java.util.stream.Collectors;

public class App implements Expr {
    public final String var;
    public final Atom[] args;

    public App(String var, Atom[] args) {
        this.var = var;
        this.args = args;
    }

    @Override
    public String toString() {
        return var + Arrays.stream(args).map(Atom::toString).collect(Collectors.joining(", ", "{", "}"));
    }
}
