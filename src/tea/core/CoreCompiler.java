package tea.core;

import tea.core.source.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CoreCompiler {

    private final Map<String, Identifier> globals;
    private final String compiled;
    private int newVar = 0;

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
        if (localEnv.stream().noneMatch(env -> env.contains(expr.name))
            && !globals.containsKey(expr.name)) {
            throw new CompileFailed("did not have `" + expr.name + "` in local env");
        }
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
        if (app.f instanceof Variable) {
            var saturatedArgs = app.args.clone();
            var caseEvalStack = new Stack<String>();
            localEnv.push(new HashSet<>());
            for(int i = 0; i < saturatedArgs.length; i++) {
                if (isValue(saturatedArgs[i])) {
                    // do nothing
                } else if (saturatedArgs[i] instanceof Application) {
                    var variable = new Variable("_hid_" + (newVar++));
                    var caseEval = "case " + compileApplication((Application)saturatedArgs[i], localEnv) +
                            " of {" + variable.name + " -> ";
                    saturatedArgs[i] = variable;
                    localEnv.peek().add(variable.name);
                    caseEvalStack.push(caseEval);
                } else {
                    throw new CompileFailed("unsupported argument: " + saturatedArgs[i]);
                }
            }
            assertVariableDefined((Variable) app.f, localEnv);
            var f = ((Variable) app.f);
            var appString = f.name + " " + argsToString(saturatedArgs, localEnv);
            localEnv.pop();
            return caseEvalStack.stream().collect(Collectors.joining(" ")) +
                appString
                + caseEvalStack.stream().map(s -> "}").collect(Collectors.joining(""));
        } else {
            throw new CompileFailed("unsupported application: " + app.toString());
        }
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
