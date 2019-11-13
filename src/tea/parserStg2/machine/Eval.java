package tea.parserStg2.machine;

import java.util.Map;

public class Eval implements Code {
    private final Variable e;
    private final Map<Variable, Value> localEnv;

    public Eval(Variable e, Map<Variable, Value> localEnv) {
        this.e = e;
        this.localEnv = localEnv;
    }
}
