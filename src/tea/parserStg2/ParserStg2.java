package tea.parserStg2;

import tea.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;

public class ParserStg2 {
    public static final String END_OF_BIND = ";";
    public static final String BIND_EQ = "=";
    private static final String START_ATOM_BLOCK = "{";
    private static final String RIGHTARROW = "->";
    private static final String END_ATOM_BLOCK = "}";
    private static final String ATOM_SEPARATOR = ",";

    private static final String KWD_LET = "let";
    private static final String KWD_CASE = "case";
    private static final String LEFT_BRACE = "(";
    private static final Object RIGHT_BRACE = ")";

    private static final Set<Object> PRIM_OPS = Set.of("+", "*", "-");

    private Set<String> CONS = Set.of(
            "MkInt");

    private final String[] tokens;
    private int ptr;

    public ParserStg2(String[] lines) {
        tokens = Tokenizer.tokenize(lines);
    }
    public Binds[] graph() throws ParsinFailed {
        ptr = 0;
        return readProgram();
    }

    private Binds[] readProgram() throws ParsinFailed {
        var binds = new ArrayList<Binds>();
        while(!isEOF()) {
            binds.add(readBinding());
            checkAndSkip(END_OF_BIND::equals);
        }
        return binds.toArray(Binds[]::new);
    }

    private Binds readBinding() throws ParsinFailed {
        var v = readVar();
        checkAndSkip(BIND_EQ::equals);
        var lf = readLambdaForm();
        return new Binds(v, lf);
    }

    private LF readLambdaForm() throws ParsinFailed {
        var freeVars = readVarBlock();
        var pi = readUpdatabilityFlag();
        var boundVars = readVarBlock();
        checkAndSkip(RIGHTARROW::equals);
        var expr = readExpression();

        return new LF(freeVars, pi, boundVars, expr);
    }

    private Expr readExpression() throws ParsinFailed {
        if (token().equals(KWD_LET)) {
            fail(token());
            return null; // TODO
        } else if (token().equals(KWD_CASE)) {
            fail(token());
            return null; // TODO
        } else {
            return readLitPrimConstrOrApp();
        }
    }

    private Expr readLitPrimConstrOrApp() throws ParsinFailed {
        if (isLiteral()) {
            return new Literal(tokenAndAdvance());
        } else if (PRIM_OPS.contains(token())) {
            var op = tokenAndAdvance();

            var args = readAtomsList();
            return new PrimOp(op, args);
        } else if (CONS.contains(token())) {
            var cons = tokenAndAdvance();
            var args = readAtomsList();
            return new Cons(cons, args);
        } else {
            if (!isIdentifier()) {
                fail("unsupported token application:" + token());
            }
            var var = tokenAndAdvance();
            var args = readAtomsList();
            return new App(var, args);
        }
    }

    private Atom[] readAtomsList() throws ParsinFailed {
        checkAndSkip(START_ATOM_BLOCK::equals);
        var args = new ArrayList<Atom>();
        while (!token().equals(END_ATOM_BLOCK)) {
            args.add(readAtom());
            checkAndSkipIf(ATOM_SEPARATOR::equals);
        }
        checkAndSkip(END_ATOM_BLOCK::equals);
        return args.toArray(Atom[]::new);
    }

    private boolean isAtom() {
        return isLiteral() || isIdentifier();
    }

    private Atom readAtom() throws ParsinFailed {
        if (isLiteral()) {
            return new Literal(tokenAndAdvance());
        } else if (isIdentifier()) {
            return readVar();
        } else {
            fail("invalid atom: " + token());
            return null;
        }
    }

    private boolean isLiteral() {
        return token().endsWith("#");
    }

    private boolean readUpdatabilityFlag() throws ParsinFailed {
        if (token().equals("\\u")) {
            advance();
            return true;
        } else if (token().equals("\\n")) {
            advance();
            return false;
        } else {
            fail("invalid updatability block `" + token() + "`");
            return false;
        }
    }

    private Identifier[] readVarBlock() throws ParsinFailed {
        checkAndSkip(START_ATOM_BLOCK::equals);
        var vars = new ArrayList<Identifier>();
        while (!token().equals(END_ATOM_BLOCK)) {
            vars.add(readVar());
            checkAndSkipIf(ATOM_SEPARATOR::equals);
        }
        checkAndSkip(END_ATOM_BLOCK::equals);
        return vars.toArray(Identifier[]::new);
    }

    private void checkAndSkipIf(Predicate<String> check) {
        if (check.test(token())) {
            advance();
        }
    }

    private Identifier readVar() throws ParsinFailed {
        if (!isIdentifier()) {
            fail("invalid variable name: `" + token() + "`");
        }
        return new Identifier(tokenAndAdvance());
    }

    private boolean isIdentifier() {
        return token().matches("^[a-zA-Z_][a-zA-Z_0-9]*$") && !CONS.contains(token());
    }

    private String tokenAndAdvance() {
        var token = token();
        advance();
        return token;
    }

    private void advance() {
        ptr ++;
    }

    private void checkAndSkip(Predicate<String> check) throws ParsinFailed {
        if (isEOF()) {
            fail("can't read past EOF!");
        }

        if (!check.test(token())) {
            fail("Wrong token at pos " + ptr + ": " + token());
        }

        advance();
    }

    private String token() {
        assert !isEOF();
        return tokens[ptr];
    }

    private void fail(String msg) throws ParsinFailed {
        throw new ParsinFailed(msg);
    }

    private boolean isEOF() {
        return ptr >= tokens.length;
    }
}
