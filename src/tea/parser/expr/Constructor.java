package tea.parser.expr;

import tea.parser.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public class Constructor extends HeapObject {
    private final Token c;
    private final List<Atom> params;

    public Constructor(Token c, List<Atom> params) {
        this.c = c;
        this.params = params;
    }

    @Override
    public String toString() {
        return "CONS<" + " " + c + " " + params.stream().map(Atom::toString).collect(Collectors.joining(" ", " ", "")) + ">";
    }
}
