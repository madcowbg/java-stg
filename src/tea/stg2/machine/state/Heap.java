package tea.stg2.machine.state;

import tea.stg2.machine.value.Addr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Heap {
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

    public void retainOnlyUsed(Set<Addr> used) {
        assert map.keySet().containsAll(used);
        var toClean = map.keySet().stream().filter(Predicate.not(used::contains)).collect(Collectors.toSet());
        for (var addr: toClean) {
            System.out.println("GC cleaning " + addr.toString());
        }
        map.keySet().retainAll(used);
        assert map.keySet().equals(used);
    }
}
