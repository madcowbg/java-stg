package tea.parserStg2.machine;

import tea.parserStg2.LF;

public class Closure {
    @Deprecated // FIXME remove
    private final LF lf;

    public Closure(LF lf) {
        this.lf = lf;
    }

    static Closure of(LF lf) {
        return new Closure(lf);
    }

    @Override
    public String toString() {
        return "Closure[" + lf.toString() + "]"; // FIXME
    }
}
