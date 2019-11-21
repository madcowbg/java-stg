package tea.core.source;

public class Literal implements Expr {

    public final int value;

    public Literal(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "" + value;
    }
}
