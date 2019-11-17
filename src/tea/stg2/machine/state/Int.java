package tea.stg2.machine.state;

import tea.stg2.machine.value.Value;
import tea.stg2.parser.expr.Literal;

public class Int implements Value {
    public final int value;

    public Int(Literal a) {
        this(a.value);
    }

    public Int(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value + "#";
    }
}
