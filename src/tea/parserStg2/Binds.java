package tea.parserStg2;

public class Binds {
    private final Identifier var;
    private final LF lf;

    public Binds(Identifier var, LF lf) {
        this.var = var;
        this.lf = lf;
    }

    @Override
    public String toString() {
        return var.name + " = " + lf + "";
    }
}
