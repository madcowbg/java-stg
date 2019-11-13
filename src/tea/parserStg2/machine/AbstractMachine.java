package tea.parserStg2.machine;


import tea.parserStg2.Binds;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class AbstractMachine {
    private int nextAddr = 0;
    public class Addr implements Value {
        int addr = nextAddr ++;

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Addr && addr == ((Addr) obj).addr;
        }

        @Override
        public String toString() {
            return "@" + Integer.toHexString(addr);
        }
    }

    private Code e;
    private Stack<Value> argumentStack = new Stack<>();
    private Stack<Continuation> returnStack = new Stack<>();
    private Stack<UpdateFrame> updateStack = new Stack<>();
    private Map<Addr, Closure> heap = new HashMap<>();
    private Map<Variable, Value> globalEnv = new HashMap<>();
    private int iter = 0;

    public AbstractMachine(Binds[] program) {
        for (var bind: program) {
            var v = Variable.of(bind.var);
            var addr = new Addr();

            globalEnv.put(v, addr);
            heap.put(addr, Closure.of(bind.lf));
        }

    }
    public Object run() throws ExecutionFailed {
        e = new Eval(Variable.MAIN, new HashMap());
        return null;
    }

    @Override
    public String toString() {
        String heapDump = heap.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n", "", "\n"));
        String globalEnvDump = globalEnv.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n", "", "\n"));
        return  "\n======== Info: =========\nIter: " + iter + "\n" +
                "========= Code: =========\n" + e + "\n" +
                "==== Argument Stack: ====\n" + Arrays.toString(argumentStack.toArray()) +
                "===== Update Stack: =====\n" + Arrays.toString(updateStack.toArray()) +
                "========= Heap: =========\n" + heapDump +
                "====== Global Env: ======\n" + globalEnvDump +
                "=========================";
    }

}

