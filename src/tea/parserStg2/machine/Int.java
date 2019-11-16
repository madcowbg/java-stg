package tea.parserStg2.machine;

import tea.parserStg2.Literal;

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
