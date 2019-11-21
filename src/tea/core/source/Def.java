package tea.core.source;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Def {
    public final Identifier f;
    public final Identifier[] args;

    public Def(Identifier f, Identifier[] args) {
        this.f = f;
        this.args = args;
    }

    @Override
    public String toString() {
        return f + " " + Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" "));
    }
}
