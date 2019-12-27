package tea.stg1.parser.expr;

import tea.stg1.parser.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public class SaturatedConstructor extends HeapObject implements Value {
    public final Token c;
    public final List<Atom> params;

    public SaturatedConstructor(Token c, List<Atom> params) {
        this.c = c;
        this.params = params;
    }

    @Override
    public String toString() {
        return "CON<" + " " + c + " " + params.stream().map(Atom::toString).collect(Collectors.joining(" ", " ", "")) + ">";
    }
}
