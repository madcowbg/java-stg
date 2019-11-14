package tea.parserStg2;

public class Variable implements Expr, Atom {
    public static Variable MAIN = new Variable("main");

    public final String name;

    public Variable(String name) {
        this.name = name;
    }

    public static Variable ofName(String name) {
        return new Variable(name);
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
