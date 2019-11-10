package tea.machine;

import tea.program.Program;
import tea.parser.expr.Value;
import tea.parser.expr.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AbstractMachine {
    public static final Variable MAIN_VAR = new Variable("main");
    private Expr e;
    private final Stack<Continuation> s = new Stack<>();

    private final Map<Variable, HeapObject> heap;
    private int freshVarCntr = 0;
    private int iter;

    public AbstractMachine(Program program) {
        heap = new HashMap<>(program.globals);
    }

    public Value run() throws ExecutionFailed {
        e = MAIN_VAR;

        while (true) {
            iter++;
            if (e instanceof Let) {
                // (LET)
                System.out.println("---------------------------------------(LET)");
                var x_prim = freshVar();
                var let_ = (Let)e;

                heap.put(x_prim, let_.obj);
                e = replaceRecursive(let_.expr, Map.of(let_.variable, x_prim));
            } else if (e instanceof Case && ((Case) e).expr instanceof Variable && heap.get((Variable)((Case) e).expr) instanceof SaturatedConstructor) {
                // (CASEON)
                System.out.println("---------------------------------------(CASECON)");

                var case_ = (Case) this.e;
                var valueConstructor = (SaturatedConstructor) heap.get(case_.expr);
                var matchedDeconstr =
                        case_.alts.stream() // start with proper deconstructors
                                .filter(DeConstructorAlt.class::isInstance)
                                .map(DeConstructorAlt.class::cast)
                                .filter(a -> valueConstructor.c.equals(a.cons))
                                .findFirst()
                                .orElseGet(() -> {
                                    throw new UnsupportedOperationException("default alternatives not implemented yet.");
                                });
                // FIXME (CASEANY)
                // alts.stream() // if not, then figure if default constructor makes sense
                //      .filter(DefaultAlternative.class::isInstance)
                //      .map(DefaultAlternative.class::cast)).findFirst();

                this.e = replaceVariables(matchedDeconstr, valueConstructor);
            } else if (e instanceof Case) {
                // (CASE)
                System.out.println("---------------------------------------(CASE)");
                var case_ = (Case) e;

                e = case_.expr;
                s.push(new CaseContinuation(case_.alts));
            } else if (!s.isEmpty() && s.peek() instanceof CaseContinuation && (e instanceof Literal || heap.get(e) instanceof Value)) {
                System.out.println("---------------------------------------(RET)");
                var cc = (CaseContinuation)s.peek();

                s.pop();
                e = new Case(e, cc.alts);
            } else if (e instanceof Variable && heap.get(e) instanceof Thunk) {
                // (THUNK)
                System.out.println("---------------------------------------(THUNK)");
                var y = (Variable) e;

                e = ((Thunk) heap.get(y)).expr;
                s.push(new Upd(y));
                heap.put(y, HeapObject.BLACKHOLE);
            } else if (!s.isEmpty() && s.peek() instanceof Upd && e instanceof Variable && heap.get(e) instanceof Value) {
                // (UPDATE)
                System.out.println("---------------------------------------(UPDATE)");

                var y = (Variable) e;
                var upd = (Upd) s.peek();

                e = y;
                s.pop();
                heap.put(upd.variable, heap.get(y));
            } else if (e instanceof FuncCall && ((FuncCall) e).f instanceof Variable && allAtoms(((FuncCall) e).exprs)) {
                // (KNOWNCALL)
                System.out.println("---------------------------------------(KNOWNCALL)");
                var call_ = ((FuncCall) e);
                var func_name = (Variable)call_.f;
                if (!(heap.get(func_name) instanceof Func)) {
                    throw new ExecutionFailed("Unrecognized function " + func_name + "!");
                }
                var func_addr = (Func)heap.get(func_name);
                if (func_addr.vars.size() != call_.exprs.size()) {
                    throw new ExecutionFailed("called with wrong arity: " + func_name + " is " + func_addr.vars.size() + " != " + (call_.exprs.size()));
                }

                var callDictionary = matchToDictionary(
                        func_addr.vars,
                        call_.exprs.stream().map(Atom.class::cast).collect(Collectors.toList()));

                e = replaceRecursive(func_addr.body, callDictionary);
            } else {
                break;
            }
        }
        if (!(e instanceof Variable) || !(heap.get(e) instanceof Value) || !s.isEmpty()) {
            throw new ExecutionFailed("Does not have rule for " + e + " in machine: \n" + this);
        }
        return (Value) heap.get(e);
    }

    private boolean allAtoms(List<Expr> exprs) {
        return exprs.stream().allMatch(Atom.class::isInstance);
    }

    private Variable freshVar() {
        return new Variable("_zvar_" + (freshVarCntr ++));
    }

    private Expr replaceVariables(Alternative alternative, SaturatedConstructor values) throws ExecutionFailed {
        if (alternative instanceof DeConstructorAlt) {
            var deConstructor = (DeConstructorAlt) alternative;
            if (deConstructor.vars.size() != values.params.size()) {
                throw new ExecutionFailed("Wrong arity: " + alternative + " of arity " + deConstructor.vars.size() + "!=" + values.params.size());
            }

            Map<Variable, Atom> dictionary = matchToDictionary(deConstructor.vars, values.params);

            return replaceRecursive(deConstructor.expr, dictionary);
        } else {
            throw new UnsupportedOperationException("default alternatives not implemented yet.");
        }
    }

    private <A, B> Map<A, B> matchToDictionary(List<A> a, List<B> b) {
        assert a.size() == b.size();
        return IntStream.range(0, a.size()).boxed()
                        .collect(Collectors.toMap(a::get, b::get));
    }

    private Expr replaceRecursive(Expr expr, Map<Variable, Atom> dictionary) throws ExecutionFailed {
        if (expr instanceof Literal) {
            return expr;
        } else if (expr instanceof Variable) {
            return dictionary.containsKey(expr) ? dictionary.get(expr) : expr;
        } else if (expr instanceof FuncCall) {
            var call_ = (FuncCall) expr;
            var params = new ArrayList<Expr>();
            for (var arg: call_.exprs) {
                params.add(replaceRecursive(arg, dictionary));
            }
            return new FuncCall(
                    replaceRecursive(call_.f, dictionary),
                    params);
        } else if (expr instanceof Let) {
            var let_ = (Let)expr;
            if (dictionary.containsKey(let_.variable)) {
                throw new ExecutionFailed("trying to bind variable second time: " + let_.variable);
            }

            return new Let(let_.variable, replaceRecursive(let_.obj, dictionary), replaceRecursive(let_.expr, dictionary));

        } else {
            throw new UnsupportedOperationException("fixme implement case free variable replacement... " + expr);
        }
    }

    private HeapObject replaceRecursive(HeapObject obj, Map<Variable, Atom> dictionary) throws ExecutionFailed {
        if (obj instanceof Thunk) {
            return new Thunk(replaceRecursive(((Thunk) obj).expr, dictionary));
        } else if (obj instanceof SaturatedConstructor) {
            var params = new ArrayList<Atom>();
            for (var arg: ((SaturatedConstructor) obj).params) {
                params.add((Atom)replaceRecursive(arg, dictionary));
            }
            return new SaturatedConstructor(((SaturatedConstructor) obj).c, params);
        } else {
            throw new UnsupportedOperationException("fixme implement case for recursive clean of obj ... " + obj);
        }
    }

    @Override
    public String toString() {
        String stackDump = s.stream().map(Continuation::toString).collect(Collectors.joining("\n", "", "\n"));
        String heapDump = heap.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n", "", "\n"));
        return  "\n======== Info: =========\nIter: " + iter + "\n" +
                "========= Expr: =========\n" + e + "\n" +
                "========= Stack: ========\n" + stackDump +
                "========= Heap: =========\n" + heapDump +
                "=========================";
    }

}

