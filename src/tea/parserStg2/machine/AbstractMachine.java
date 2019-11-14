package tea.parserStg2.machine;


import tea.parserStg2.*;

import java.util.*;
import java.util.stream.Collectors;

public class AbstractMachine {
    private int nextAddr = 0;

    private Code e;
    private Stack<Value> argumentStack = new Stack<>();
    private Stack<Continuation> returnStack = new Stack<>();
    private Stack<UpdateFrame> updateStack = new Stack<>();
    private Heap heap = new Heap();
    private Environment globalEnv = Environment.newEmpty();
    private int iter = 0;

    public AbstractMachine(Binds[] program) {
        for (var bind: program) {
            var v = bind.var;
            var addr = newAddr();

            globalEnv.modify(v, addr);
            heap.store(addr, Closure.ofCode(bind.lf));
        }

    }

    private Addr newAddr() {
        return new Addr(nextAddr ++);
    }

    public Expr run() throws ExecutionFailed {
        e = new Eval(new Application(Variable.MAIN, new Atom[0]), Environment.newEmpty());
        iter = 0;
        while(true) {
            iter++;
            if (e instanceof Eval && eAsEval().e instanceof Application && val(eAsEval().localEnv, globalEnv, ((Application) eAsEval().e).f) instanceof Addr ) {
                debug("Rule: Eval (f x) r");
                var eval = eAsEval();
                var app = (Application) eval.e;
                var f = app.f;
                var a = (Addr) val(eval.localEnv, globalEnv, f);

                for (var x: app.args) {
                    argumentStack.push(val(eval.localEnv, globalEnv, x));
                }
                e = new Enter(a);
            } else if (e instanceof Enter && heap.has(eAsEnter().a)) {
                debug("Rule: Enter a");

                var closure = heap.at(eAsEnter().a);
                var xs = closure.codePointer.boundVars;
                assert argumentStack.size() >= xs.length : "can't execute with insufficient argument stack!";

                // get arguments from stack...
                var ws_a = new LinkedList<Value>();
                for(var x: xs) {
                    ws_a.push(argumentStack.pop());
                }
                var localEnv = Environment.newWith(closure.codePointer.freeVars, closure.freeVars);
                localEnv.modifiedWith(closure.codePointer.boundVars, ws_a);
                e = new Eval(closure.codePointer.expr, localEnv);
            } else {
                break;
            }
        }

        debug(this.toString());
        if (!(e instanceof Eval)) {
            throw new ExecutionFailed("Execution stopped at improper state: " + e);
        }

        return eAsEval().e;
    }

    private Enter eAsEnter() {
        return (Enter) e;
    }

    private void debug(String msg) {
        System.out.println(msg);
    }

    private Eval eAsEval() {
        return (Eval) e;
    }

    private static Value val(Environment local, Environment global, Atom a) throws ExecutionFailed {
        if (a instanceof Literal) {
            return new Int((Literal) a);
        } else {
            assert a instanceof Variable;
            var _v = (Variable)a;
            if (local.contains(_v)) {
                return local.valueOf(_v);
            } else if (global.contains(_v)) {
                return global.valueOf(_v);
            } else {
                throw new ExecutionFailed("Requesting undefined value of " + a + " in local:\n" + local.toString() + "\n or global:\n" + global.toString());
            }
        }
    }

    @Override
    public String toString() {
        return  "\n======== Info: =========\nIter: " + iter + "\n" +
                "========= Code: =========\n" + e + "\n" +
                "==== Argument Stack: ====\n" + Arrays.toString(argumentStack.toArray()) +
                "===== Update Stack: =====\n" + Arrays.toString(updateStack.toArray()) +
                "========= Heap: =========\n" + heap.toString() +
                "====== Global Env: ======\n" + globalEnv.toString() +
                "=========================";
    }

}

class Heap {
    private Map<Addr, Closure> map = new HashMap<>();

    public void store(Addr addr, Closure closure) {
        map.put(addr, closure);
    }

    public Closure at(Addr addr) {
        return map.get(addr);
    }

    @Override
    public String toString() {
        return map.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n", "", "\n"));
    }

    public boolean has(Addr a) {
        return map.containsKey(a);
    }
}

class Int implements Value {
    private final int value;

    public Int(Literal a) {
        this.value = a.value;
    }
}

class Addr implements Value {
    public final int addr;

    Addr(int addr) {
        this.addr = addr;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Addr && addr == ((Addr) obj).addr;
    }

    @Override
    public String toString() {
        return "@" + Integer.toHexString(addr);
    }
}

