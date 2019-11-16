package tea.parserStg2;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Cons<Atom> implements Expr {
    public final String cons;
    public final Atom[] args;

    public Cons(String cons, Atom[] args) {
        this.cons = cons;
        this.args = args;
    }

    @Override
    public String toString() {
        return cons + " " + Arrays.stream(args).map(Atom::toString).collect(Collectors.joining(",", "{", "}"));
    }
}
