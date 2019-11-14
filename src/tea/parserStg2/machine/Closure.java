package tea.parserStg2.machine;

import tea.parserStg2.Expr;
import tea.parserStg2.LF;
import tea.parserStg2.Variable;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class Closure implements Expr {
    public LF codePointer;
    public final Map<Variable, Value> freeVars;

    private Closure(LF codePointer, Map<Variable, Value> freeVars) {
        this.codePointer = codePointer;
        this.freeVars = freeVars;
    }

    static Closure ofCode(LF lf) {
        return new Closure(lf, Collections.emptyMap());
    }

    public static Closure ofCodeAndVars(LF lf, Map<Variable, Value> freeVars) {
        return new Closure(lf, freeVars);
    }

    @Override
    public String toString() {
        return "Closure[(" + codePointer.toString() + ")@(" + freeVars.entrySet().stream().map(e -> e.getKey() + "=>" +e.getValue()).collect(Collectors.joining(";")) + ")]";
    }
}
