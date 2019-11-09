package tea.parser;

import tea.parser.expr.*;
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
        while (!ptr.isEOF()) {
            bindings.add(readBinding(ptr));
        }

        return bindings.toArray(Binding[]::new);

    }

    /**
     * var = obj
     */
    private static Binding readBinding(TokenPointer ptr) {
        var variable = ptr.advanceAfterCheck(Token::isVariableName, " is not a valid variable name!");
        ptr.skipAfterCheck(Token::isEquals, " must be `=`!");

        var valueExpr = readHeapObject(ptr);

        if (!ptr.isEOF()) {
            ptr.skipAfterCheck(Token::isEndOfBinding, " is not a recognized end of binding!");
        }

        return new Binding(new Variable(variable), valueExpr);
    }

    /**
     * Heap objects
     * CONS(C x y ...)        - saturated constructor
     * FUN(x1 ... xn -> e)    - Function (arity = n >= 1)
     */
    private static HeapObject readHeapObject(TokenPointer ptr) {
        if (ptr.isConstructorHeap()) {
            ptr.skipAfterCheck(Token::isConstructorHeap, "expected `CONS`");
            var cons = readSaturatedConstructor(ptr);
            ptr.skipAfterCheck(Token::isEndBrace, "expected `)`");
            return cons;
        } else if (ptr.isFunctionHeap()) {
            ptr.skipAfterCheck(Token::isFunctionHeap, "expected `FUN(`");
            var fun = readFunction(ptr);
            ptr.skipAfterCheck(Token::isEndBrace, "expected `)`");
            return fun;
        } else {
            ptr.fail(" is not a valid heap object type!");
            return null; // unreachable
        }
    }

    private static Func readFunction(TokenPointer ptr) {
        List<Variable> vars = new ArrayList<>();

        while (!ptr.isRightArrow()) {
            var name = ptr.advanceAfterCheck(Token::isVariableName, " is not a valid variable!");
            vars.add(new Variable(name));
        }

        ptr.skipAfterCheck(Token::isRightArrow, " is not `->`!"); // skip `->`

        var e = readExpression(ptr);

        ptr.checkCurrent(Token::isEndBrace, " is not `)`!");

        return new Func(vars, e);
    }

    private static Expr readExpression(TokenPointer ptr) {
        if (ptr.isAtom()) {
            var atom = readAtom(ptr);
            return atom;
        }
        if (ptr.isCase()) {
            ptr.advance(); // skip `case`

            return readCase(ptr);
        }

        ptr.fail(" is not an expression!!!");
        return null; // unreachable
    }

    private static Case readCase(TokenPointer ptr) {
        var e = readExpression(ptr);

        // skip `of`
        ptr.skipAfterCheck(Token::isOf, " but expected `of`!");
        // skip `{`
        ptr.skipAfterCheck(Token::isOpenCurly, ", expected `{`!");

        var alts = new ArrayList<Alternative>();
        while (!ptr.isClosedCurly()) {
            alts.add(readAlternative(ptr));

            ptr.checkCurrent(Token::isEndOfAlt, ", expected `}` or `;`.");

            if (ptr.isSemicolon()) {
                ptr.advance(); // skip ;
            }
        }

        ptr.skipAfterCheck(Token::isClosedCurly, ", expected `}`"); // skip `}`
        return new Case(e, alts);
    }

    /**
     * C x1 ... xn -> e       - (n >= 0)
     * x -> e                 - default alternative
     */
    private static Alternative readAlternative(TokenPointer ptr) {
        if (ptr.isConstructor()) {
            var cons = ptr.advanceAfterCheck(Token::isConstructor, "WTF?");

            List<Variable> vars = new ArrayList<>();
            while (!ptr.isRightArrow()) {
                var name = ptr.advanceAfterCheck(Token::isVariableName, ", expected a variable name or `->`.");
                vars.add(new Variable(name));
            }
            ptr.advance(); // skip ->

            var expr = readExpression(ptr);

            ptr.checkCurrent(Token::isEndOfAlt, ", expected `;` or `}`.");

            return new DeConstructor(cons, vars, expr);
        } else if (ptr.isVariableName()) {
            var name = ptr.advanceAfterCheck(Token::isVariableName, ", expected variable name");

            ptr.skipAfterCheck(Token::isRightArrow, ", expected `->`."); // skip `->`

            var expr = readExpression(ptr);
            ptr.checkCurrent(Token::isEndOfAlt, ", expected `;` or `}`.");

            return new DefaultAlternative(new Variable(name), expr);
        } else {
            ptr.fail(", expected constructor or default alternative.");
            return null; // unreachable
        }
    }

    /**
     * C x y ... - saturated constructor call, n >= 0
     */
    private static Constructor readSaturatedConstructor(TokenPointer ptr) {
        var cons = ptr.advanceAfterCheck(Token::isConstructor, " is not a valid constructor!");

        List<Atom> atoms = new ArrayList<>();
        while (!ptr.isEndBrace()) {
            ptr.checkCurrent(Token::isAtom, " is not a an atom!");

            atoms.add(readAtom(ptr));
        }
        return new Constructor(cons, atoms);
    }

    private static Atom readAtom(TokenPointer ptr) {
        if (ptr.isLitteral()) {
            return new Literal(ptr.advanceAfterCheck(Token::isLitteral, "WTF?").litteralValue());
        } else if (ptr.isVariableName()) {
            return new Variable(ptr.advanceAfterCheck(Token::isVariableName, "WTF?"));
        } else {
            ptr.fail(" is not atom!");
            return null; // unreachable
        }
    }

}
