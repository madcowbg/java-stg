package tea.parser.expr;

import tea.HeapValue;

public class HeapObject implements HeapValue {

    public static final HeapObject BLACKHOLE = new HeapObject() {
        @Override
        public String toString() {
            return "BLACKHOLE";
        }
    };
}
