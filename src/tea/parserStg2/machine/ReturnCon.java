package tea.parserStg2.machine;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ReturnCon implements Code {
    public final Data cons;
    public final Value[] vals;

    public ReturnCon(Data cons, Value[] vals) {
        this.cons = cons;
        this.vals = vals;
    }

    @Override
    public String toString() {
        return "ReturnCon " + cons + " " + Arrays.stream(vals).map(Object::toString).collect(Collectors.joining(" "));
    }
}
