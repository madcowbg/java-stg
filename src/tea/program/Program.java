package tea.program;

import tea.parser.expr.*;
import tea.parser.token.primops.PrimOpExpr;

import java.util.*;

public class Program {
    public final Map<Variable, HeapObject> globals;

    public Program(Binding[] graph) throws LoadingFailed {
        this.globals = readTopLevelBindings(graph);

        for (var global : globals.values()) {
            validate(global, new Stack<>());
        }
    }

    private void validate(HeapObject global, Stack<Variable> locals) throws LoadingFailed {
        if (global instanceof Thunk) {
            validate(((Thunk) global).expr, locals);
        } else if (global instanceof SaturatedConstructor) {
            debug("Inspecting Saturated constructor " + global);
            var _sc = (SaturatedConstructor) global;
            // TODO check constructor is available: if (!globals.containsKey(_sc.c))
            for (var p: _sc.params) {
                validate(p, locals);
            }
        } else if (global instanceof Func) {
            debug("Inspecting function " + global);
            var _func = (Func) global;
            validateInContext(_func.body, _func.vars, locals);
        } else {
            throw new LoadingFailed("Unsupported object type: " + global);
        }
    }

    private void validateInContext(Expr body, List<Variable> vars, Stack<Variable> locals) throws LoadingFailed {
        for (var param: vars) {
            validateNotDefined(param, locals);
            locals.push(param);
        }
        validate(body, locals);
        for (var ignored : vars) {
            locals.pop();
        }
    }

    private void validateNotDefined(Variable variableToBind, Stack<Variable> locals) throws LoadingFailed {
        if (globals.containsKey(variableToBind)) {
            throw new LoadingFailed("Function parameter can't override global: " + variableToBind);
        } else if (locals.contains(variableToBind)) {
            throw new LoadingFailed("Function parameter can't override bound local: " + variableToBind);
        }
    }

    private void validate(Expr expr, Stack<Variable> locals) throws LoadingFailed {
        if (expr instanceof Atom) {
            validateAtom((Atom)expr, locals);
        } else if (expr instanceof Case) {
            var _case = (Case)expr;
            validate(_case.expr, locals);
            for (var alt: _case.alts) {
                validate(alt, locals);
            }
            var defAlts = _case.alts.stream().filter(DefaultAlternative.class::isInstance).toArray();
            if (defAlts.length > 1) {
                throw new LoadingFailed("Can't have more than one default alternative: " + Arrays.toString(defAlts));
            }
        } else if (expr instanceof Let) {
            var _let = (Let) expr;
            validateNotDefined(_let.variable, locals);

            locals.push(_let.variable);
            validate(_let.obj, locals); // left side
            validate(_let.expr, locals); // right side
            locals.pop();
        } else if (expr instanceof FuncCall) {
            var _func = (FuncCall) expr;

            // FIXME lambdas not implemented yet...
            if (!(_func.f instanceof Variable)) {
                throw new LoadingFailed("FIXME lambda application not yet supported: " + _func.f);
            }
            var func_expr = (Variable)_func.f;
            if (!(globals.get(func_expr) instanceof Func)) {
                throw new LoadingFailed("FIXME partial application not yet supported: " + _func.f);
            }

            for (var arg: _func.exprs) {
                validate(arg, locals);
            }
        } else if (expr instanceof PrimOpExpr) {
            debug("primitive operation: " + expr);

            var op = (PrimOpExpr) expr;
            if (op.args.size() != op.arity()) {
                throw new LoadingFailed("Arity of " + op + " != arg size " + op.args.size());
            }
            for (var arg: op.args) {
                if (!(arg instanceof Atom)) {
                    throw new LoadingFailed("Primitive op needs atoms, but got " + arg);
                }
                validateAtom((Atom)arg, locals);
            }
        } else {
            throw new LoadingFailed("Unsupported expression type: " + expr);
        }
    }

    private void validateAtom(Atom expr, Stack<Variable> locals) throws LoadingFailed {
        if (expr instanceof Literal) {
            debug("used literal: " + expr);
        } else if (expr instanceof Variable) {
            var _var = (Variable)expr;
            if (globals.containsKey(_var)) {
                debug("used global var: " + _var);
            } else if (locals.contains(_var)) { // if (locals?)
                debug("used local var: " + _var);
            } else {
                throw new LoadingFailed("Expression contains unrecognized global or local variable: " + expr);
            }
        } else {
            throw new LoadingFailed("Unsupported atom type: " + expr);
        }
    }

    private void debug(String msg) {
        System.out.println(msg);
    }

    private void validate(Alternative alt, Stack<Variable> locals) throws LoadingFailed {
        if (alt instanceof DeConstructorAlt) {
            debug("validating deconstructor alternative: " + alt);
            var _decon = (DeConstructorAlt) alt;
            // TODO validate that _decon.cons is actually a constructor name
            validateInContext(_decon.expr, _decon.vars, locals);
        } else if (alt instanceof DefaultAlternative) {
            var _def = (DefaultAlternative) alt;
            validateNotDefined(_def.variable, locals);
            validateInContext(_def.expr, List.of(_def.variable), locals);
        } else {
            throw new LoadingFailed("Unsupported case alternative: " + alt);
        }
    }

    private static Map<Variable, HeapObject> readTopLevelBindings(Binding[] graph) throws LoadingFailed {
        var globals = new HashMap<Variable, HeapObject>();
        for(var b: graph) {
            if (globals.containsKey(b.variable)) {
                throw new LoadingFailed("attempt to redefine variable " + b.variable);
            }
            globals.put(b.variable, b.valueExpr);
        }
        return globals;
    }
}
