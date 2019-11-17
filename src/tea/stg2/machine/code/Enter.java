package tea.stg2.machine.code;

import tea.stg2.machine.value.Addr;

public class Enter implements Code {
    public final Addr a;

    public Enter(Addr a) {
        this.a = a;
    }

    @Override
    public String toString() {
        return "Enter " + a;
    }
}
