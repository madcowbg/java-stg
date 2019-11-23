package tea.stg2.machine.value;

public class Addr implements Value {
    public final int addr;

    public Addr(int addr) {
        this.addr = addr;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Addr && addr == ((Addr) obj).addr;
    }

    @Override
    public int hashCode() {
        return addr;
    }

    @Override
    public String toString() {
        return "@" + Integer.toHexString(addr);
    }
}
