package tea;

import tea.parser.expr.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class AbstractMachine {

    private ExprValue e;
    private final Stack<Continuation> s = new Stack<>();
    private final Map<Variable, HeapValue> heap = new HashMap<>();


    public AbstractMachine(Binding[] program) throws ExecutionFailed {
        for(var b: program) {
            if (heap.containsKey(b.variable)) {
                throw new ExecutionFailed("attempt to redefine variable " + b.variable);
            }
            heap.put(b.variable, b.valueExpr);
        }
    }

    public Literal run() throws ExecutionFailed {
        e = new Variable("main");

        while (!resultIsCalculated()) {
            if (e instanceof Variable && heap.containsKey(e) && heap.get(e) instanceof Thunk) {       // (THUNK)
                var x = (Variable)e;
                var thunk = (Thunk) heap.get(x);

                e = thunk.expr;
                s.push(new Upd(x));
                heap.put(x, HeapObject.BLACKHOLE);
            } else if (e instanceof Literal && !s.isEmpty() && s.peek() instanceof Upd) {
                var y = (Literal) e;
                var upd = (Upd)s.peek();

                e = y;
                s.pop();
                heap.put(upd.variable, y);
            } else {
                throw new ExecutionFailed("Does not have rule for " + e + " considering" + this);
            }
        }
        assert e instanceof Literal;
        return (Literal) e;
    }

    private boolean resultIsCalculated() {
        return s.isEmpty() && e instanceof Literal;
    }

    @Override
    public String toString() {
        return "State:\n" +
                "Expr: " + e + "\n" +
                "Stack:\n" + s.stream().map(Continuation::toString).collect(Collectors.joining("\n")) +
                "Heap:\n" + heap.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n"));
    }

}

class Upd extends Continuation {
    final Variable variable;

    public Upd(Variable variable) {
        this.variable = variable;
    }
}

class Continuation {
}
