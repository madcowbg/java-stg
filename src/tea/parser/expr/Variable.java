package tea.parser.expr;

import tea.parser.token.Token;

public class Variable implements Atom {
    private final String name;

    public Variable(Token name) {
        this(name.text);
    }

    public Variable(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "V<" + name + ">";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Variable && name.equals(((Variable) obj).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
