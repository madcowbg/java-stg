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

    public Object run() throws ExecutionFailed {
        e = new Eval(new Application(Variable.MAIN, new Atom[0]), Environment.newEmpty());
        iter = 0;
        while(true) {
            iter++;
            if (e instanceof Eval && eAsEval().e instanceof Application && val(eAsEval().localEnv, globalEnv, ((Application) eAsEval().e).f) instanceof Addr ) {
                debug("(1)  Eval (f x) r");
                var eval = eAsEval();
                var app = (Application) eval.e;
                var f = app.f;
                var a = (Addr) val(eval.localEnv, globalEnv, f);

                for (var x: app.args) {
                    argumentStack.push(val(eval.localEnv, globalEnv, x));
                }
                e = new Enter(a);
            } else if (e instanceof Enter && heap.has(eAsEnter().a)) {
                debug("(2)  Enter a");

                var closure = heap.at(eAsEnter().a);
                var xs = closure.codePointer.boundVars;
                if (!(argumentStack.size() >= xs.length)) {
                    debug(this.toString());
                    throw new ExecutionFailed("can't execute with insufficient argument stack!");
                }

                // get arguments from stack...
                var ws_a = new LinkedList<Value>();
                for(var x: xs) {
                    ws_a.push(argumentStack.pop());
                }
                var localEnv = Environment.newWith(closure.codePointer.freeVars, closure.freeVars);
                localEnv.modifiedWith(closure.codePointer.boundVars, ws_a.toArray(Value[]::new));
                e = new Eval(closure.codePointer.expr, localEnv);
            } else if (e instanceof Eval && eAsEval().e instanceof Let) {
                debug("(3)  Eval (let ... in ...)");
                var let = (Let) eAsEval().e;

                Addr[] addrs = Arrays.stream(let.binds).map(b -> newAddr()).toArray(Addr[]::new);
                var internalEnv = eAsEval().localEnv.copyWithExtension(Arrays.stream(let.binds).map(b -> b.var).toArray(Variable[]::new), addrs);
                var localEnvRhs = let.isRec ? internalEnv : eAsEval().localEnv;

                for (int i = 0; i < addrs.length; i++) {
                    heap.store(addrs[i], Closure.ofCodeAndVars(let.binds[i].lf, localEnvRhs.valuesOf(let.binds[i].lf.freeVars)));
                }

                e = new Eval(let.expr, internalEnv);
            } else if (e instanceof Eval && eAsEval().e instanceof Case) {
                debug("(4)  Eval (case e of alts) rho");
                var case_ = (Case) eAsEval().e;

                returnStack.push(new Continuation(case_.alts, eAsEval().localEnv));
                e = new Eval(case_.expr, eAsEval().localEnv);
            } else if (e instanceof Eval && eAsEval().e instanceof Cons) {
                debug("(5)  Eval (c xs) rho");
                var cons = (Cons<Atom>)eAsEval().e;
                e = new ReturnCon(new Data(cons.cons), vals(eAsEval().localEnv, globalEnv, cons.args));
            } else if (e instanceof ReturnCon && !returnStack.isEmpty()) {
                var c = ((ReturnCon) e).cons;
                var ws = ((ReturnCon) e).vals;

                var cont = returnStack.pop();

                var altOpt = findMatchingConstructor(c, ws, cont.alts);
                if (altOpt.isEmpty()) {
                    throw new ExecutionFailed("did not find matching constructor or default!");
                }

                if (altOpt.get() instanceof AlgAlt) {
                    debug("(6)  ReturnCon c ws");
                    var alt = (AlgAlt) altOpt.get();
                    cont.localEnv.modifiedWith(alt.cons.args, ws);
                    e = new Eval(alt.expr, cont.localEnv);
                } else {
                    assert altOpt.get() instanceof DefaultAlt;
                    var alt = (DefaultAlt) altOpt.get();
                    if (alt.var.isEmpty()) {
                        debug("(7)  ReturnCon c ws");
                        e = new Eval(alt.expr, cont.localEnv);
                    } else {
                        // FIXME implement (8) - case default
                        throw new UnsupportedOperationException("default alternative not implemented for constructors!");
                    }
                }
            } else if (e instanceof Eval && eAsEval().e instanceof Literal) {
                debug("(9)  Eval k #Int");
                e = new ReturnInt(new Int(((Literal) eAsEval().e)));
            } else if (e instanceof Eval && eAsEval().e instanceof Application && eAsEval().localEnv.valueOf(((Application) eAsEval().e).f) instanceof Int) {
                debug("(10) Eval (f {}) (f -> Int k)");
                e = new ReturnInt((Int) eAsEval().localEnv.valueOf(((Application) eAsEval().e).f));
            } else if (e instanceof ReturnInt && returnStack.size() > 0) {

                var ri = (ReturnInt) e;
                var cont = returnStack.pop();

                var altOpt = Arrays.stream(cont.alts).filter(a -> a.matchPrim(ri.k)).findFirst();
                if (altOpt.isEmpty()) {
                    throw new ExecutionFailed("did not find matching primitive constructor or default!");
                }
                if (altOpt.get() instanceof PrimAlt) {
                    debug("(11) ReturnInt k");
                    var alt = (PrimAlt) altOpt.get();
                    e = new Eval(alt.expr, cont.localEnv);
                } else if (altOpt.get() instanceof DefaultAlt) {
                    var alt = (DefaultAlt) altOpt.get();
                    if (alt.var.isPresent()) {
                        debug("(12) ReturnInt k");
                        cont.localEnv.modify(alt.var.get(), ri.k);
                        e = new Eval(alt.expr, cont.localEnv);
                    } else {
                        debug("(13) ReturnInt k");
                        e = new Eval(alt.expr, cont.localEnv);
                    }
                }
            } else if (e instanceof Eval && eAsEval().e instanceof PrimOp) {
                debug("(14) Eval PrimOp");
                var op = (PrimOp)eAsEval().e;

                var args = eAsEval().localEnv.literalsOf(op.args);
                e = new ReturnInt(calc(op.op, args));
            } else {
                break;
            }
        }

        if (argumentStack.size() > 0) {
            debug(this.toString());
            throw new ExecutionFailed("Execution stopped with non-empty argument stack: " + Arrays.toString(argumentStack.toArray()));
        }
        if (updateStack.size() > 0) {
            debug(this.toString());
            throw new ExecutionFailed("Execution stopped with non-empty update stack: " + Arrays.toString(updateStack.toArray()));
        }
        if (!(e instanceof ReturnInt || e instanceof ReturnCon)) {
            debug(this.toString());
            throw new ExecutionFailed("Execution stopped at improper state: " + e);
        }

        debug(this.toString());
        return e;
    }

    private Int calc(String op, Int[] args) throws ExecutionFailed {
        switch(op) {
            case "+":
                return new Int(Arrays.stream(args).mapToInt(i -> i.value).sum());
            case "*":
                return new Int(Arrays.stream(args).mapToInt(i -> i.value).reduce((a, b) -> a+b).orElse(1));
            default:
                throw new ExecutionFailed("Unrecognized primitive: " + op);
        }
    }

    private Optional<Alt> findMatchingConstructor(Data c, Value[] xs, Alt[] alts) {
        return Arrays.stream(alts)
                .filter(a -> a.matchCon(c, xs))
                .findFirst();
    }

    private Value[] vals(Environment localEnv, Environment globalEnv, Atom[] args) throws ExecutionFailed {
        var res = new Value[args.length];
        for (int i = 0; i < args.length; i++) {
            res[i] = val(localEnv, globalEnv, args[i]);
        }
        return res;
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
                "==== Argument Stack: ====\n" + Arrays.toString(argumentStack.toArray()) + "\n" +
                "===== Update Stack: =====\n" + Arrays.toString(updateStack.toArray()) + "\n" +
                "========= Heap: =========\n" + heap.toString() + "\n" +
                "====== Global Env: ======\n" + globalEnv.toString() + "\n" +
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

