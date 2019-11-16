package tea.parserStg2.machine;

import tea.parserStg2.Atom;
import tea.parserStg2.Literal;
import tea.parserStg2.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Environment {
    private Map<Variable, Value> vals;

    private Environment(Map<Variable, Value> vals) {
        this.vals = vals;
    }

    public static Environment newEmpty() {
        return new Environment(new HashMap<>());
    }

    public static Environment newWith(Variable[] vars, Map<Variable, Value> values) {
        return new Environment(
                Arrays.stream(vars).filter(values::containsKey).collect(Collectors.toMap(Function.identity(), values::get)));
    }

    public void modify(Variable variable, Value value) {
        this.vals.put(variable, value);
    }

    public boolean contains(Variable a) {
        return vals.containsKey(a);
    }

    public Value valueOf(Variable a) {
        assert vals.containsKey(a);
        return vals.get(a);
    }

    @Override
    public String toString() {
        return toStr("\n");
    }

    public String toStr(String c) {
        return vals.entrySet().stream().map(e -> e.getKey() + " -> " + e.getValue()).collect(Collectors.joining(c));
    }

    public void modifiedWith(Variable[] vars, Value[] vals) {
        assert vars.length == vals.length;
        IntStream.range(0, vars.length).forEach(i -> this.vals.put(vars[i], vals[i]));
    }

    public Environment copyWithExtension(Variable[] vars, Addr[] addrs) {
        assert vars.length == addrs.length;
        var newVals = new HashMap<>(this.vals); // fixme could optimize this
        IntStream.range(0, vars.length).forEach(i -> newVals.put(vars[i], addrs[i]));
        return new Environment(newVals);
    }

    public Map<Variable, Value> valuesOf(Variable[] vars) {
        return Arrays.stream(vars).filter(vals::containsKey).collect(Collectors.toMap(Function.identity(), vals::get));
    }

    public Int[] literalsOf(Atom[] args) throws ExecutionFailed {
        var res = new Int[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Literal) {
                res[i] = new Int((Literal) args[i]);
            } else if (args[i] instanceof Variable) {
                var arg = vals.get((Variable)args[i]);
                if (arg instanceof Int) {
                    res[i] = (Int)arg;
                } else {
                    throw new ExecutionFailed("local environments maps " + args[i] + " to " + arg + " instead of to Int.");
                }
            } else {
                throw new ExecutionFailed("unrecognized argument type: " + args[i]);
            }
        }
        return res;
    }
}
