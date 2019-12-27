package tea.stg1.parser;

import tea.tokenizer.Tokenizer;
import tea.stg1.parser.expr.*;
import tea.stg1.parser.token.Token;
import tea.stg1.parser.token.TokenPointer;
import tea.stg1.parser.token.primops.PrimOpExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static tea.stg1.parser.token.Token.*;

public class ParserStg1 {

    public static Binding[] readToGraph(String[] lines) {
        var tokens = Tokenizer.tokenize(lines);
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
        ptr.skipAfterCheck("=");

        var valueExpr = readHeapObject(ptr);

        if (!ptr.isEOF()) {
            ptr.skipAfterCheck(";");
        }

        return new Binding(new Variable(variable), valueExpr);
    }

    /**
     * Heap objects
     * CONS(C x y ...)        - saturated constructor
     * FUN(x1 ... xn -> e)    - Function (arity = n >= 1)
     */
    private static HeapObject readHeapObject(TokenPointer ptr) {
        if (ptr.is(KWD_THUNK)) {
            ptr.skipAfterCheck(KWD_THUNK);
            var expr = readExpression(ptr);
            ptr.skipAfterCheck(")");
            return new Thunk(expr);
        } else if (ptr.is(KWD_CON)) {
            ptr.skipAfterCheck(KWD_CON);
            var cons = readSaturatedConstructor(ptr);
            ptr.skipAfterCheck(")");
            return cons;
        } else if (ptr.is(Token.KWD_FUN)) {
            ptr.skipAfterCheck(Token.KWD_FUN);
            var fun = readFunction(ptr);
            ptr.skipAfterCheck(")");
            return fun;
        } else {
            ptr.fail(" is not a valid heap object type!");
            return null; // unreachable
        }
    }

    private static Func readFunction(TokenPointer ptr) {
        List<Variable> vars = new ArrayList<>();

        while (!ptr.is("->")) {
            var name = ptr.advanceAfterCheck(Token::isVariableName, " is not a valid variable!");
            vars.add(new Variable(name));
        }

        ptr.skipAfterCheck("->");

        var e = readExpression(ptr);

        ptr.checkCurrent(")");

        return new Func(vars, e);
    }

    private static Expr readExpression(TokenPointer ptr) {
        if (ptr.is("(")) {
            ptr.skipAfterCheck("(");
            Expr expr;
            if (ptr.isPrimOp()) {
                var op = ptr.advanceAfterCheck(t -> PRIMOPS.containsKey(t.text), ", but is not a primitive operation");
                ArrayList<Expr> args = readRemainingArguments(ptr);
                expr = new PrimOpExpr(op, args);
            } else {
                ArrayList<Expr> exprs = readRemainingArguments(ptr);
                expr = new FuncCall(exprs.get(0), exprs.stream().skip(1).collect(Collectors.toList()));
            }
            ptr.skipAfterCheck(")");
            return expr;
        } else if(ptr.is(KWD_LET)) {
            ptr.skipAfterCheck(KWD_LET);
            var variable = ptr.advanceAfterCheck(Token::isVariableName, ", expected variable name");
            ptr.skipAfterCheck("=");
            var obj = readHeapObject(ptr);
            ptr.skipAfterCheck(KWD_IN);
            var expr = readExpression(ptr);
            return new Let(new Variable(variable), obj, expr);
        } else if (ptr.is(KWD_CASE)) {
            ptr.skipAfterCheck(KWD_CASE);
            return readCase(ptr);
        } else if (ptr.isAtom()) {
            var atom = readAtom(ptr);
            return atom;
        } else {
            ptr.fail(" is not an expression!!!");
            return null; // unreachable
        }
    }

    private static ArrayList<Expr> readRemainingArguments(TokenPointer ptr) {
        var exprs = new ArrayList<Expr>();
        while (!ptr.is(")")) {
            exprs.add(readExpression(ptr));
        }
        return exprs;
    }

    private static Case readCase(TokenPointer ptr) {
        var e = readExpression(ptr);

        ptr.skipAfterCheck(KWD_OF);
        ptr.skipAfterCheck("{");

        var alts = new ArrayList<Alternative>();
        while (!ptr.is("}")) {
            alts.add(readAlternative(ptr));

            ptr.checkCurrent(ParserStg1::isEndOfAlt, ", expected `}` or `;`.");

            ptr.skipIf(";");
        }

        ptr.skipAfterCheck("}");
        return new Case(e, alts);
    }

    private static boolean isEndOfAlt(Token token) {
        return token.text.equals(";") || token.text.equals("}");
    }

    /**
     * C x1 ... xn -> e       - (n >= 0)
     * x -> e                 - default alternative
     */
    private static Alternative readAlternative(TokenPointer ptr) {
        if (ptr.isConstructor()) {
            var cons = ptr.advanceAfterCheck(Token::isConstructor, "WTF?");

            List<Variable> vars = new ArrayList<>();
            while (!ptr.is("->")) {
                var name = ptr.advanceAfterCheck(Token::isVariableName, ", expected a variable name or `->`.");
                vars.add(new Variable(name));
            }
            ptr.advance(); // skip ->

            var expr = readExpression(ptr);

            ptr.checkCurrent(ParserStg1::isEndOfAlt, ", expected `;` or `}`.");

            return new DeConstructorAlt(cons, vars, expr);
        } else if (ptr.isVariableName()) {
            var name = ptr.advanceAfterCheck(Token::isVariableName, ", expected variable name");

            ptr.skipAfterCheck("->");

            var expr = readExpression(ptr);
            ptr.checkCurrent(ParserStg1::isEndOfAlt, ", expected `;` or `}`.");

            return new DefaultAlternative(new Variable(name), expr);
        } else {
            ptr.fail(", expected constructor or default alternative.");
            return null; // unreachable
        }
    }

    /**
     * C x y ... - saturated constructor call, n >= 0
     */
    private static SaturatedConstructor readSaturatedConstructor(TokenPointer ptr) {
        var cons = ptr.advanceAfterCheck(Token::isConstructor, " is not a valid constructor!");

        List<Atom> atoms = new ArrayList<>();
        while (!ptr.is(")")) {
            ptr.checkCurrent(Token::isAtom, " is not an atom!");

            atoms.add(readAtom(ptr));
        }
        return new SaturatedConstructor(cons, atoms);
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
