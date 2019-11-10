package tea.parser.expr;

import tea.ExprValue;

public class HeapObject implements ExprValue {

    public static final HeapObject BLACKHOLE = new HeapObject() {
        @Override
        public String toString() {
            return "BLACKHOLE";
        }
    };
}
