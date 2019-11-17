package tea.stg2.machine.state;

import tea.stg2.machine.value.Addr;

import java.util.HashMap;
import java.util.Map;
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
}
