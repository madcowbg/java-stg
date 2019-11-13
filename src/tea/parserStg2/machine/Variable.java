package tea.parserStg2.machine;

import tea.parserStg2.Identifier;

public class Variable {
    public static Variable MAIN = new Variable("main");

    private final String name;

    public Variable(String name) {
        this.name = name;
    }

    public static Variable of(Identifier var) {
        return new Variable(var.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Variable && this.name.equals(((Variable) obj).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
