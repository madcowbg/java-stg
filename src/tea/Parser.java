package tea;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {


    public static String[] tokenize(String[] lines) {
        return Stream.of(lines)
                .filter(Predicate.not(String::isBlank))
                .map(String::trim)
                .filter(Predicate.not(Parser::isComment))
                .collect(Collectors.joining(" ")) // assume space at end of lines
                .replace(";", " ; ") // simplify semicolon tokenization
                .replace("(", "( ")
                .replace(")", " ) ")
                .replace('\t', ' ') // simplify whitespaces
                .replaceAll("\\s{2,}", " ") // cleanup whitespaces
                .split("\\s");
    }

    private static boolean isComment(String s) {
        return s.startsWith("#");
    }

    public static Binding[] readToGraph(String[] lines) {
        var tokens = tokenize(lines);
        var ptr = new TokenPointer(tokens);

        return new Binding[]{readBinding(ptr)};

    }

    /**
     * var = obj
     * @param ptr
     * @return
     */
    private static Binding readBinding(TokenPointer ptr) {
        var variable = ptr.current();
        if (!variable.isVariableName()) {
            throw new RuntimeException(ptr.current() + " is not a valid variable name!");
        }

        ptr.advance();

        var eq = ptr.current();
        if (!eq.isEquals()) {
            throw new RuntimeException(eq + " must be `=`!");
        }

        ptr.advance();

        var valueExpr = readHeapObject(ptr);

        if (!ptr.current().isEndOfBinding()) {
            throw new RuntimeException(ptr.current() + " is not a recognized end of binding!");
        }
        return new Binding(new Variable(variable), valueExpr);
    }

    /**
     * Heap objects
     * CONS(C x y ...) - saturated constructor
     *
     * @param ptr
     * @return
     */
    private static HeapObject readHeapObject(TokenPointer ptr) {
        if (ptr.current().isConstructorHeap()) {
            ptr.advance(); // skip CONS(
            var cons = readSaturatedConstructor(ptr);
            ptr.advance(); // skip )
            return cons;
        } else {
            throw new RuntimeException(ptr.current() + " is not a valid heap object type!"); // FIXME add...
        }
    }

    /**
     * C x y ... - saturated constructor call, n >= 0
     * @param ptr
     * @return
     */
    private static Constructor readSaturatedConstructor(TokenPointer ptr) {
        var cons = ptr.current();
        if (cons.isConstructor()) {
            ptr.advance();
            List<Atom> atoms = new ArrayList<>();

            while (!ptr.current().isEndBrace()) {
                if (!ptr.current().isAtom()) {
                    throw new RuntimeException(ptr.current() + " is not a an atom!");
                }

                atoms.add(new Atom(ptr.current()));
                ptr.advance();
            }
            return new Constructor(cons, atoms);
        } else {
            throw new RuntimeException(cons + " is not a valid constructor!");
        }
    }

    private static class Binding {
        private final Variable variable;
        private final HeapObject valueExpr;

        public Binding(Variable variable, HeapObject valueExpr) {
            this.variable = variable;
            this.valueExpr = valueExpr;
        }

        @Override
        public String toString() {
            return "B<" + variable + ", " + valueExpr.toString() + ">";
        }

    }

    private static class Variable {
        private final String name;

        public Variable(Token name) {
            this.name = name.inner();
        }

        @Override
        public String toString() {
            return "V<" + name + ">";
        }
    }

    private static class Atom {
        private final int value; // fixme other atom types...

        public Atom(Token current) {
            this.value = current.atomValue();
        }

        @Override
        public String toString() {
            return "A<" + value + ">";
        }
    }

    private static class HeapObject {

    }

    private static class Constructor extends HeapObject {
        private final Token c;
        private final List<Atom> params;

        public Constructor(Token c, List<Atom> params) {
            this.c = c;
            this.params = params;
        }

        @Override
        public String toString() {
            return "CONS(" + " " + c + " " + params.stream().map(Atom::toString).collect(Collectors.joining(" ", " ", ""));
        }
    }
}
