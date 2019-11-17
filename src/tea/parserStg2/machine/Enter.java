package tea.parserStg2.machine;

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
