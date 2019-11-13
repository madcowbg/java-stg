package tea.parserStg2;

public class Identifier implements Atom {
    public final String name;

    public Identifier(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ID`" + name + "`";
    }
}
