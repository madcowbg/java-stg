package tea.stg2.machine.state;

import tea.stg2.parser.LambdaForm;
import tea.stg2.parser.expr.Variable;
import tea.stg2.machine.value.Value;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class Closure {
    public LambdaForm codePointer;
    public final Map<Variable, Value> freeVars;

    private Closure(LambdaForm codePointer, Map<Variable, Value> freeVars) {
        this.codePointer = codePointer;
        this.freeVars = freeVars;
    }

    public static Closure ofCode(LambdaForm lf) {
        return new Closure(lf, Collections.emptyMap());
    }

    public static Closure ofCodeAndVars(LambdaForm lf, Map<Variable, Value> freeVars) {
        return new Closure(lf, freeVars);
    }

    @Override
    public String toString() {
        return "Closure[(" + codePointer.toString() + ")@(" + freeVars.entrySet().stream().map(e -> e.getKey() + "=>" +e.getValue()).collect(Collectors.joining(";")) + ")]";
    }
}
