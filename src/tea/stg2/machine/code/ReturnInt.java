package tea.stg2.machine.code;

import tea.stg2.machine.value.Int;

public class ReturnInt implements Code {
    public final Int k;

    public ReturnInt(Int k) {
        this.k = k;
    }

    @Override
    public String toString() {
        return "ReturnInt " + k;
    }
}
