package tea.stg2.machine;

public class RuntimeExecutionFailed extends RuntimeException {
    public RuntimeExecutionFailed(String msg) {
        super(msg);
    }
}
