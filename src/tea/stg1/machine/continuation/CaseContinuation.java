package tea.stg1.machine.continuation;

import tea.stg1.parser.expr.Alternative;

import java.util.List;
import java.util.stream.Collectors;

public class CaseContinuation implements Continuation {
    public List<Alternative> alts;

    public CaseContinuation(List<Alternative> alts) {
        this.alts = alts;
    }

    @Override
    public String toString() {
        return "case * of {" + alts.stream().map(Object::toString).collect(Collectors.joining(";")) + "}";
    }
}
