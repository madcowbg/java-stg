package tea.stg2.machine;

import tea.stg2.parser.ParsingFailed;

public class ExecutionFailed extends Exception {
    public ExecutionFailed(String msg) {
        super(msg);
    }

    public ExecutionFailed(Exception e) {
        super(e);
    }
}
