package tea.parserStg2;

import tea.parserStg2.machine.Data;
import tea.parserStg2.machine.Value;

public interface Alt {
    boolean matches(Data c, Value[] xs);
}
