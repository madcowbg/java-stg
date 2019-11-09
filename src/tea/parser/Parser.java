package tea.parser;

import tea.parser.token.Token;
import tea.parser.token.TokenPointer;

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

        List<Binding> bindings = new ArrayList<>();
        while (!ptr.current().isEOF()) {
            bindings.add(readBinding(ptr));
        }

        return bindings.toArray(Binding[]::new);

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

        ptr.advance();
        return new Binding(new Variable(variable), valueExpr);
    }

    /**
     * Heap objects
     * CONS(C x y ...)        - saturated constructor
     * FUN(x1 ... xn -> e)    - Function (arity = n >= 1)
     */
    private static HeapObject readHeapObject(TokenPointer ptr) {
        if (ptr.current().isConstructorHeap()) {
            ptr.advance(); // skip CONS(
            var cons = readSaturatedConstructor(ptr);
            ptr.advance(); // skip )
            return cons;
        } else if (ptr.current().isFunctionHeap()) {
            ptr.advance(); // skip FUN(
            var fun = readFunction(ptr);
            ptr.advance(); // skip )
            return fun;
        } else {
            throw new RuntimeException(ptr.current() + " is not a valid heap object type!"); // FIXME add...
        }
    }

    private static Func readFunction(TokenPointer ptr) {
        List<Variable> vars = new ArrayList<>();

        while (!ptr.current().isRightArrow()) {
            if (!ptr.current().isVariableName()) {
                throw new RuntimeException(ptr.current() + " is not a valid variable!");
            }
            vars.add(new Variable(ptr.current()));
            ptr.advance();
        }

        ptr.advance(); // skip `->`

        var e = readExpression(ptr);

        if (!ptr.current().isEndBrace()) {
            throw new RuntimeException(ptr.current() + " is not `)`!");
        }

        return new Func(vars, e);
    }

    private static Expr readExpression(TokenPointer ptr) {
        if (ptr.current().isAtom()) {
            var atom = readAtom(ptr);
            ptr.advance();
            return atom;
        } if (ptr.current().isCase()) {
            ptr.advance();
            var case_ = readCase(ptr);

            return case_;
        } else { // FIXME add more expression types
            throw new RuntimeException(ptr.current()  + " is not an expression!!!");
        }
    }

    private static Case readCase(TokenPointer ptr) {
        var e = readExpression(ptr);

        if (!ptr.current().isOf()) {
            throw new RuntimeException("got " + ptr.current() + ", expected `of`!");
        }
        ptr.advance(); // skip `of`

        if (!ptr.current().isOpenCurly()) {
            throw new RuntimeException("got " + ptr.current() + ", expected `{`!");
        }
        ptr.advance(); // skip {

        var alts = new ArrayList<Alternative>();
        while (!ptr.current().isClosedCurly()) {
            alts.add(readAlternative(ptr));
            if (!ptr.current().isEndOfAlt()) {
                throw new RuntimeException("got " + ptr.current() + ", expected `}` or `;`.");
            }
            if (ptr.current().isSemicolon()) {
                ptr.advance(); // skip ;
            }
        }

        ptr.advance(); // skip `}`
        return new Case(e, alts);
    }

    /**
     * C x1 ... xn -> e       - (n >= 0)
     * x -> e                 - default alternative
     */
    private static Alternative readAlternative(TokenPointer ptr) {
        if (ptr.current().isConstructor()) {
            var cons = ptr.current();

            ptr.advance();

            List<Variable> vars = new ArrayList<>();
            while (!ptr.current().isRightArrow()) {
                if (!ptr.current().isVariableName()) {
                    throw new RuntimeException("got " + ptr.current() + ", expected a variable name or `->`.");
                }
                vars.add(new Variable(ptr.current()));
                ptr.advance();
            }
            ptr.advance(); // skip ->

            var expr = readExpression(ptr);
            if (!ptr.current().isEndOfAlt()) {
                throw new RuntimeException("got " + ptr.current() + ", expected `;` or `}`.");
            }

            return new DeConstructor(cons, vars, expr);
        } else if (ptr.current().isVariableName()) {
            var variable = new Variable(ptr.current());
            ptr.advance(); // skip var
            if (!ptr.current().isRightArrow()) {
                throw new RuntimeException("got " + ptr.current() + ", expected `->`.");
            }
            ptr.advance(); // skip `->`

            var expr = readExpression(ptr);
            if (!ptr.current().isEndOfAlt()) {
                throw new RuntimeException("got " + ptr.current() + ", expected `;` or `}`.");
            }

            return new DefaultAlternative(variable, expr);
        } else {
            throw new RuntimeException("got " + ptr.current() + ", expected constructor or default alternative.");
        }
    }

    /**
     * C x y ... - saturated constructor call, n >= 0
     *
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

                atoms.add(readAtom(ptr));
                ptr.advance();
            }
            return new Constructor(cons, atoms);
        } else {
            throw new RuntimeException(cons + " is not a valid constructor!");
        }
    }

    private static Atom readAtom(TokenPointer ptr) {
        if (ptr.current().isLitteral()) {
            return new Litteral(ptr.current().litteralValue());
        } else if (ptr.current().isVariableName()) {
            return new Variable(ptr.current());
        } else {
            throw new RuntimeException(ptr.current()  + " is not atom!");
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

    private static class Variable implements Atom {
        private final String name;

        public Variable(Token name) {
            this.name = name.inner();
        }

        @Override
        public String toString() {
            return "V<" + name + ">";
        }
    }

    interface Atom extends Expr {

    }

    private static class Litteral implements Atom {
        private final int value;

        public Litteral(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Lit<" + value + ">";
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
            return "CONS<" + " " + c + " " + params.stream().map(Atom::toString).collect(Collectors.joining(" ", " ", "")) + ">";
        }
    }

    private static class Func extends HeapObject {
        private final List<Variable> vars;
        private final Expr body;

        public Func(List<Variable> vars, Expr body) {
            this.vars = vars;
            this.body = body;
        }

        @Override
        public String toString() {
            return "FUN<" + vars.stream().map(Variable::toString).collect(Collectors.joining(" ", " ", "")) + " -> " + body.toString() + ">";
        }
    }

    private interface Expr {
    }

    private static class Alternative {
    }

    private static class Case implements Expr {
        private final Expr expr;
        private final ArrayList<Alternative> alts;

        public Case(Expr expr, ArrayList<Alternative> alts) {
            this.expr = expr;
            this.alts = alts;
        }

        @Override
        public String toString() {
            return "case <" + expr + ", " + alts.stream().map(Alternative::toString).collect(Collectors.joining(";", "Alts<", ">")) + ">";
        }
    }

    private static class DeConstructor extends Alternative {
        private final Token cons;
        private final List<Variable> vars;
        private final Expr expr;

        public DeConstructor(Token cons, List<Variable> vars, Expr expr) {
            this.cons = cons;
            this.vars = vars;
            this.expr = expr;
        }

        @Override
        public String toString() {
            return "DC<" + cons + vars.stream().map(Variable::toString).collect(Collectors.joining(" ", " ", "")) + " -> " + expr + ">";
        }
    }

    private static class DefaultAlternative extends Alternative {
        private final Variable variable;
        private final Expr expr;

        public DefaultAlternative(Variable variable, Expr expr) {
            this.variable = variable;
            this.expr = expr;
        }

        @Override
        public String toString() {
            return "Def<" + variable + " -> " + expr + ">";
        }
    }
}
