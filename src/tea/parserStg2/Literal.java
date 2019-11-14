package tea.parserStg2;

public class Literal implements Expr, Atom {
    public final int value;

    public Literal(String value) {
        this.value = Integer.parseInt(value.replace("#", ""));
    }

    @Override
    public String toString() {
        return "#`" + value + "`";
    }
}
