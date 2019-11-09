package tea.parser.expr;

import tea.parser.token.Token;

public class Variable implements Atom {
    private final String name;

    public Variable(Token name) {
        this.name = name.inner();
    }

    @Override
    public String toString() {
        return "V<" + name + ">";
    }
}
