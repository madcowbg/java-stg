package tea.stg1.parser.expr;

public class HeapObject {

    public static final HeapObject BLACKHOLE = new HeapObject() {
        @Override
        public String toString() {
            return "BLACKHOLE";
        }
    };
}
