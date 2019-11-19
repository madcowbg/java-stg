package tea.stg2.machine.state;

import tea.stg2.parser.LambdaForm;
import tea.stg2.parser.expr.Variable;
import tea.stg2.machine.value.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Closure {
    public LambdaForm codePointer;
    public final Value[] freeVarValues;

    private Closure(LambdaForm codePointer, Map<Variable, Value> freeVars) {
        this.codePointer = codePointer;
        this.freeVarValues = Arrays.stream(codePointer.freeVars).map(freeVars::get).toArray(Value[]::new);
    }

    public static Closure ofCode(LambdaForm lf) {
        return new Closure(lf, Collections.emptyMap());
    }

    public static Closure ofCodeAndVars(LambdaForm lf, Map<Variable, Value> freeVars) {
        return new Closure(lf, freeVars);
    }

    @Override
    public String toString() {
        return "Closure[(" + codePointer.toString() + ")@(" + Arrays.stream(freeVarValues).map(Objects::toString).collect(Collectors.joining(" ")) + ")]";
    }
}
