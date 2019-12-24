package tea.stg2.machine;


import tea.stg2.parser.*;
import tea.stg2.machine.code.*;
import tea.stg2.machine.state.*;
import tea.stg2.machine.value.Addr;
import tea.stg2.machine.value.Int;
import tea.stg2.machine.value.Value;
import tea.stg2.parser.alt.AlgAlt;
import tea.stg2.parser.alt.Alt;
import tea.stg2.parser.alt.DefaultAlt;
import tea.stg2.parser.alt.PrimAlt;
import tea.stg2.parser.expr.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AbstractMachine {
    private int nextAddr = 0;

    private Code e;
    private Stack<Value> argumentStack = new Stack<>();
    private Stack<Continuation> returnStack = new Stack<>();
    private Stack<UpdateFrame> updateStack = new Stack<>();
    private Heap heap = new Heap();
    private Environment globalEnv = Environment.newEmpty();
    private int iter = 0;

    public AbstractMachine(Bind[] program) {
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
            } else if (e instanceof Enter && heap.has(eAsEnter().a) && !heap.at(eAsEnter().a).codePointer.isUpdatable && (argumentStack.size() >= heap.at(eAsEnter().a).codePointer.boundVars.length)) {
                debug("(2)  Enter a # a is non-updatable");

                var closure = heap.at(eAsEnter().a);
                // get arguments from stack...
                var ws_a = new LinkedList<Value>();
                for(var x: closure.codePointer.boundVars) {
                    ws_a.push(argumentStack.pop());
                }
                var localEnv = Environment.newWith(closure.codePointer.freeVars, closure.freeVarValues);
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
            } else if (e instanceof ReturnCon && returnStack.size() > 0) {
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
            } else if (e instanceof Enter && heap.has(eAsEnter().a) && heap.at(eAsEnter().a).codePointer.isUpdatable && heap.at(eAsEnter().a).codePointer.boundVars.length == 0) {
                debug("(15)  Enter a # a is updatable without bound vars");

                var closure = heap.at(eAsEnter().a);

                updateStack.push(new UpdateFrame(argumentStack, returnStack, eAsEnter().a));
                argumentStack = new Stack<>();
                returnStack = new Stack<>();

                var localEnv = Environment.newWith(closure.codePointer.freeVars, closure.freeVarValues);
                e = new Eval(closure.codePointer.expr, localEnv);
            } else if (e instanceof ReturnCon && returnStack.isEmpty() && !updateStack.isEmpty()) {
                debug("(16)  ReturnCon # with empty returnStack");
                assert argumentStack.isEmpty() : "trying ReturnCon with empty return stack but non-empty argument stack!";

                var frame = updateStack.pop();

                var rc = (ReturnCon) e;
                var ws = rc.vals;
                var vs = IntStream.range(0, ws.length).mapToObj(i -> Variable.ofName("_hv_" + i)).toArray(Variable[]::new);
                var boundValues = IntStream.range(0, ws.length).boxed().collect(Collectors.toMap(i -> vs[i], i -> ws[i]));
                Closure closure = null;
                try {
                    closure = Closure.ofCodeAndVars(new LambdaForm(vs, false, new Variable[0], new Cons<>(rc.cons.cons, vs)), boundValues);
                } catch (ParsingFailed parsingFailed) {
                    throw new ExecutionFailed(parsingFailed);
                }

                heap.store(frame.a, closure);
                argumentStack = frame.argumentStack;
                returnStack = frame.returnStack;
                e = new ReturnCon(rc.cons, rc.vals);
            } else if (e instanceof Enter && returnStack.isEmpty() && !updateStack.isEmpty() && heap.has(eAsEnter().a) && !heap.at(eAsEnter().a).codePointer.isUpdatable && (argumentStack.size() < heap.at(eAsEnter().a).codePointer.boundVars.length)) {
                debug("(17)  Enter a # with insufficient returnStack");

                var closure = heap.at(eAsEnter().a);
                var frame = updateStack.pop();

                // as ++ as_u
                var as = argumentStack;
                argumentStack = frame.argumentStack;
                for (var a: as) {
                    argumentStack.push(a);
                }
                returnStack = frame.returnStack;
                var f = Variable.ofName("_hidden_f");

                var xs_1 = IntStream.range(0, as.size()).mapToObj(i -> Variable.ofName("_hv_" + i)).toArray(Variable[]::new);
                var f_xs1 = Stream.concat(Stream.of(f), Stream.of(xs_1)).toArray(Variable[]::new);

                var boundVals = new HashMap<Variable, Value>();
                boundVals.put(f, eAsEnter().a);
                for (int i = 0; i < xs_1.length; i++) {
                    boundVals.put(xs_1[i], as.get(i));
                }

                try {
                    heap.store(frame.a, Closure.ofCodeAndVars(new LambdaForm(f_xs1, false, new Variable[0], new Application(f, xs_1)), boundVals));
                } catch (ParsingFailed parsingFailed) {
                    throw new ExecutionFailed(parsingFailed);
                }
                e = new Enter(eAsEnter().a);
            } else {
                break;
            }

            gc();
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

    private void gc() {
        Set<Addr> endPoints = new HashSet<>(globalEnv.addrs());
        if (e instanceof Eval) {
            endPoints.addAll(((Eval) e).localEnv.addrs());
        }

        if (e instanceof Enter) {
            endPoints.add(((Enter) e).a);
        }

        if (e instanceof ReturnCon) {
            endPoints.addAll(Arrays.stream(((ReturnCon) e).vals).filter(Addr.class::isInstance).map(Addr.class::cast).collect(Collectors.toSet()));
        }

        endPoints.addAll(continuationAddresses(returnStack));
        endPoints.addAll(argumentAddresses(argumentStack));
        endPoints.addAll(updateStack.stream().map(u -> u.argumentStack).map(AbstractMachine::argumentAddresses).flatMap(Set::stream).collect(Collectors.toSet()));
        endPoints.addAll(updateStack.stream().map(u -> u.returnStack).map(AbstractMachine::continuationAddresses).flatMap(Set::stream).collect(Collectors.toSet()));
        endPoints.addAll(updateStack.stream().map(u -> u.a).collect(Collectors.toSet()));

        var visited = new HashSet<Addr>();
        var next = new Stack<Addr>();
        next.addAll(endPoints);
        while (next.size() > 0) {
            var u = next.pop();
            assert heap.at(u) != null : "can't get at address " + u;
            var closureReferences = Arrays.stream(heap.at(u).freeVarValues)
                    .filter(Addr.class::isInstance)
                    .map(Addr.class::cast)
                    .filter(Predicate.not(visited::contains)).collect(Collectors.toSet());
            next.addAll(closureReferences);
            visited.add(u);
        }

        heap.retainOnlyUsed(visited);
    }

    private static Set<Addr> argumentAddresses(Stack<Value> argumentStack) {
        return argumentStack.stream().filter(Addr.class::isInstance).map(Addr.class::cast).collect(Collectors.toSet());
    }

    private static Set<Addr> continuationAddresses(Stack<Continuation> returnStack) {
        return returnStack.stream().flatMap(c -> c.localEnv.addrs().stream()).collect(Collectors.toSet());
    }

    private Int calc(String op, Int[] args) throws ExecutionFailed {
        switch(op) {
            case "+":
                return new Int(Arrays.stream(args).mapToInt(i -> i.value).sum());
            case "*":
                return new Int(Arrays.stream(args).mapToInt(i -> i.value).reduce((a, b) -> a+b).orElse(1));
            case "-":
                if (args.length != 2) {
                    throw new ExecutionFailed("- is a binary op, got " + args.length + " parameters!");
                }
                return new Int(args[0].value - args[1].value);
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

