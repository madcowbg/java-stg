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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Literal && ((Literal) obj).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
