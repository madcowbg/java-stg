package tea.parserStg2.machine;

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
