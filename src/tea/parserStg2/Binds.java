package tea.parserStg2;

public class Binds {
    public final Variable var;
    public final LF lf;

    public Binds(Variable var, LF lf) {
        this.var = var;
        this.lf = lf;
    }

    @Override
    public String toString() {
        return var.name + " = " + lf + "";
    }
}
