package tea.parser.expr;

import tea.HeapValue;

public class Literal implements Atom, HeapValue {
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
