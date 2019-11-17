package tea.stg2.parser.alt;

import tea.stg2.machine.state.Data;
import tea.stg2.machine.value.Value;

public interface Alt {
    boolean matchCon(Data c, Value[] xs);

    boolean matchPrim(Value k);
}
