package tea.core;

import tea.core.source.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoreCompiler {

    private final Map<String, Identifier> globals;
    private final String compiled;
    private int newVarCtr = 0;

    public CoreCompiler(Bind[] binds) throws CompileFailed {
        this.globals = Arrays.stream(binds).map(b -> b.var.f).collect(Collectors.toMap(i -> i.name, Function.identity()));

        var source = new StringBuilder();
        for (var bind: binds) {
            source.append(compile(bind));
            source.append(";\n");
        }
        compiled = source.toString();
    }

    private String compile(Bind bind) throws CompileFailed {
        assert globals.containsKey(bind.var.f.name);
        var freeVarExpr = "{}";
        var boundVarExpr = Arrays.stream(bind.var.args).map(i -> i.name).collect(Collectors.joining(" ", "{", "}"));
        var localEnv = new Stack<Set<String>>();
        localEnv.push(Arrays.stream(bind.var.args).map(i -> i.name).collect(Collectors.toSet()));
        var expr = compileExpr(bind.expr, localEnv);
        return bind.var.f.name + " = " + freeVarExpr + "\\n" + boundVarExpr + " -> " + expr ;
    }

    private String compileExpr(Expr expr, Stack<Set<String>> localEnv) throws CompileFailed {
        if (expr instanceof Literal) {
            return compileLiteral((Literal) expr);
        } else if (expr instanceof Variable) {
            return compileVariable((Variable) expr, localEnv);
        } else if (expr instanceof Application) {
            return compileApplication((Application)expr, localEnv);
        } else if (expr instanceof PrimOpApp) {
            return compilePrimOpApp((PrimOpApp)expr, localEnv);
        } else {
            throw new CompileFailed("unrecognized expression: " + expr);
        }
    }

    private String compileVariable(Variable expr, Stack<Set<String>> localEnv) throws CompileFailed {
        assertVariableDefined(expr, localEnv);
        return expr.name + " {}";
    }

    private void assertVariableDefined(Variable expr, Stack<Set<String>> localEnv) throws CompileFailed {
        if (!isVariableDefined(expr, localEnv)) {
            throw new CompileFailed("did not have `" + expr.name + "` in local env");
        }
    }

    private boolean isVariableDefined(Variable var, Stack<Set<String>> localEnv) {
        return globals.containsKey(var.name) || localEnv.stream().anyMatch(env -> env.contains(var.name));
    }

    private String compileLiteral(Literal expr) {
        return "" + expr.value + "#";
    }

    private String compilePrimOpApp(PrimOpApp app, Stack<Set<String>> localEnv) {
        if (Arrays.stream(app.args).allMatch(CoreCompiler::isValue)) {
            var op = app.op;
            return op + " " + argsToString(app.args, localEnv);
        } else {
            throw new CompileFailed("unsupported application: " + app.toString());
        }
    }

    private static boolean isValue(Expr expr) {
        return ((Predicate<Expr>)Variable.class::isInstance).or(Literal.class::isInstance).test(expr);
    }

    private String compileApplication(Application app, Stack<Set<String>> localEnv) throws CompileFailed {
        if (app.f instanceof Application) {
            // expand with let expression
            var fvar = newVar();
            var fapp = (Application)app.f;

            var boundVars = matchingVars(app.f, v -> isVariableDefined(v, localEnv));
            var freeVars = matchingVars(app.f, v -> !globals.containsKey(v.name) && !boundVars.contains(v.name));

            localEnv.push(Set.of(fvar.name));
            var result = String.format(
                    "let {%s = {%s} \\n {%s} -> %s} in %s",
                    fvar.name,
                    String.join(" ", boundVars),
                    String.join(" ", freeVars),
                    compileApplication(fapp, localEnv),
                    compileActualApplication(fvar, app.args, localEnv));
            localEnv.pop();
            return result;
        } else if (app.f instanceof Variable) {
            var appf = ((Variable) app.f);
            return compileActualApplication(appf, app.args, localEnv);
        } else {
            throw new CompileFailed("unsupported application: " + app.toString());
        }
    }

    private Set<String> matchingVars(Expr expr, Predicate<Variable> condition) {
        if (expr instanceof Literal) {
            return Collections.emptySet();
        } else if (expr instanceof Variable) {
            if (condition.test((Variable) expr)) {
                return Set.of(((Variable) expr).name);
            } else {
                return Collections.emptySet();
            }
        } else if (expr instanceof Application) {
            return Stream.concat(
                    matchingVars(((Application) expr).f, condition).stream(),
                    Arrays.stream(((Application) expr).args).flatMap(a -> matchingVars(a, condition).stream()))
                    .collect(Collectors.toSet());
        } else {
            throw new CompileFailed("can't get free vars of unrecognized expression: " + expr);
        }
    }

    private String compileActualApplication(Variable appf, Expr[] args, Stack<Set<String>> localEnv) {
        args = args.clone();
        var caseEvalStack = new Stack<String>();
        localEnv.push(new HashSet<>());
        for(int i = 0; i < args.length; i++) {
            if (isValue(args[i])) {
                // do nothing
            } else if (args[i] instanceof Application) {
                var variable = newVar();
                var caseEval = "case " + compileApplication((Application)args[i], localEnv) +
                        " of {" + variable.name + " -> ";
                args[i] = variable;
                localEnv.peek().add(variable.name);
                caseEvalStack.push(caseEval);
            } else {
                throw new CompileFailed("unsupported argument: " + args[i]);
            }
        }
        assertVariableDefined(appf, localEnv);
        var appString = appf.name + " " + argsToString(args, localEnv);
        localEnv.pop();
        return caseEvalStack.stream().collect(Collectors.joining(" "))
                    + appString
                    + caseEvalStack.stream().map(s -> "}").collect(Collectors.joining(""));
    }

    private Variable newVar() {
        return new Variable("_hid_" + (newVarCtr++));
    }

    private String argsToString(Expr[] args, Stack<Set<String>> localEnv) {
        return Arrays.stream(args)
                .map(compileValue(localEnv))
                .collect(Collectors.joining(" ", "{", "}"));
    }

    private Function<Expr, String> compileValue(Stack<Set<String>> localEnv) {
        return e -> {
            if (e instanceof Literal) {
                return compileLiteral((Literal) e);
            } else if (e instanceof Variable) {
                assertVariableDefined((Variable) e, localEnv);
                return ((Variable) e).name;
            } else {
                throw new CompileFailed("unrecognized value expression: " + e);
            }
        };
    }

    public String compile() {
        return compiled;
    }
}
