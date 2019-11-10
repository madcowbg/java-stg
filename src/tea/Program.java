package tea;

import tea.parser.expr.Binding;
import tea.parser.expr.HeapObject;
import tea.parser.expr.Variable;

import java.util.HashMap;
import java.util.Map;

public class Program {
    public Map<Variable, HeapObject> globals = new HashMap<>();

    public Program(Binding[] graph) throws LoadingFailed {

        for(var b: graph) {
            if (globals.containsKey(b.variable)) {
                throw new LoadingFailed("attempt to redefine variable " + b.variable);
            }
            globals.put(b.variable, b.valueExpr);
        }
    }
}
