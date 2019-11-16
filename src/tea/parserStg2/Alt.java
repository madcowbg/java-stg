package tea.parserStg2;

import tea.parserStg2.machine.Data;
import tea.parserStg2.machine.Value;

public interface Alt {
    boolean matchCon(Data c, Value[] xs);

    boolean matchPrim(Value k);
}
