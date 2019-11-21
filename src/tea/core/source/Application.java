package tea.core.source;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Application implements Expr {
    public final Expr f;
    public final Expr[] args;

    public Application(Expr f, Expr[] args) {
        this.f = f;
        this.args = args;
    }

    @Override
    public String toString() {
        return "(" + f.toString() + " " + Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" ")) + ")";
    }
}
