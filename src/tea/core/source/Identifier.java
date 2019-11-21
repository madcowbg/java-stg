package tea.core.source;

public class Identifier {
    public final String name;

    public Identifier(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "I`" + name + "`";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Identifier && name.equals(((Identifier) obj).name);
    }
}
