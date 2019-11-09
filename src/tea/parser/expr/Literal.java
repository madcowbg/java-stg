package tea.parser.expr;

public class Literal implements Atom {
    private final int value;

    public Literal(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Lit<" + value + ">";
    }
}
