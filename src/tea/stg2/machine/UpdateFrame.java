package tea.stg2.machine;

import tea.stg2.machine.state.Continuation;
import tea.stg2.machine.value.Addr;
import tea.stg2.machine.value.Value;

import java.util.Stack;

public class UpdateFrame {
    public final Stack<Value> argumentStack;
    public final Stack<Continuation> returnStack;
    public final Addr a;

    public UpdateFrame(Stack<Value> argumentStack, Stack<Continuation> returnStack, Addr a) {
        this.argumentStack = argumentStack;
        this.returnStack = returnStack;
        this.a = a;
    }
}
