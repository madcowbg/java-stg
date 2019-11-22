package tea.stg1.parser.expr;

public class Thunk extends HeapObject {
    public Expr expr;

    public Thunk(Expr expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "THUNK(" + expr + ")";
    }
}
