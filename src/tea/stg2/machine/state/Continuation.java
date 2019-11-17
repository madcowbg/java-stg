package tea.stg2.machine.state;

import tea.stg2.parser.alt.Alt;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Continuation {
    public final Alt[] alts;
    public final Environment localEnv;

    public Continuation(Alt[] alts, Environment localEnv) {
        this.alts = alts;
        this.localEnv = localEnv;
    }

    @Override
    public String toString() {
        return "CaseCont(" + Arrays.stream(alts).map(Object::toString).collect(Collectors.joining(";")) + "@" + localEnv.toStr(",") + ")";
    }
}
