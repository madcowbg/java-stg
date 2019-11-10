package tea.program;

import tea.parser.expr.*;

import java.util.*;

public class Program {
    public final Map<Variable, HeapObject> globals;

    public Program(Binding[] graph) throws LoadingFailed {
        this.globals = readTopLevelBindings(graph);

        new Validator(globals).validate();
    }

    private static Map<Variable, HeapObject> readTopLevelBindings(Binding[] graph) throws LoadingFailed {
        var globals = new HashMap<Variable, HeapObject>();
        for(var b: graph) {
            if (globals.containsKey(b.variable)) {
                throw new LoadingFailed("attempt to redefine variable " + b.variable);
            }
            globals.put(b.variable, b.valueExpr);
        }
        return Collections.unmodifiableMap(globals);
    }
}

