package tea.stg2.parser;

import tea.stg2.parser.alt.AlgAlt;
import tea.stg2.parser.alt.Alt;
import tea.stg2.parser.alt.DefaultAlt;
import tea.stg2.parser.alt.PrimAlt;
import tea.stg2.parser.expr.*;
import tea.tokenizer.AnnotatedToken;
import tea.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class Parser {
    public static final String END_OF_BIND = ";";
    public static final String BIND_EQ = "=";
    private static final String START_ATOM_BLOCK = "{";
    private static final String RIGHTARROW = "->";
    private static final String END_ATOM_BLOCK = "}";
    private static final String ATOM_SEPARATOR = ",";

    private static final String KWD_LET = "let";
    private static final String KWD_LETREC = "letrec";
    private static final String KWD_IN = "in";
    private static final String KWD_CASE = "case";
    private static final String KWD_OF = "of";
    private static final String LEFT_BRACE = "(";
    private static final Object RIGHT_BRACE = ")";

    private static final Set<Object> PRIM_OPS = Set.of("+", "*", "-");
    private static final String START_LET_BLOCK = "{";
    private static final String END_LET_BLOCK = "}";
    private static final String START_ALT_BLOCK = "{";
    private static final String END_ALT_BLOCK = "}";
    private static final String KWD_DEFAULT = "default";

    private Set<String> CONS = Set.of(
            "MkInt", "Nil", "Cons");

    private Set<String> KWDS = Set.of(
            KWD_LET, KWD_LETREC, KWD_IN,
            KWD_CASE, KWD_OF, KWD_DEFAULT
    );

    private final AnnotatedToken[] tokens;
    private int ptr;

    public Parser(String[] lines) {
        tokens = Tokenizer.tokenizeAnnotated(lines);
    }
    public Bind[] graph() throws ParsingFailed {
        ptr = 0;
        return readProgram();
    }

    private Bind[] readProgram() throws ParsingFailed {
        var binds = new ArrayList<Bind>();
        while(!isEOF()) {
            binds.add(readBinding());
            checkAndSkip(END_OF_BIND::equals);
        }
        return binds.toArray(Bind[]::new);
    }

    private Bind readBinding() throws ParsingFailed {
        var v = readVar();
        checkAndSkip(BIND_EQ::equals);
        var lf = readLambdaForm();
        return new Bind(v, lf);
    }

    private LambdaForm readLambdaForm() throws ParsingFailed {
        var freeVars = readVarBlock();
        var pi = readUpdatabilityFlag();
        var boundVars = readVarBlock();
        checkAndSkip(RIGHTARROW::equals);
        var expr = readExpression();

        return new LambdaForm(freeVars, pi, boundVars, expr);
    }

    private Expr readExpression() throws ParsingFailed {
        if (token().equals(KWD_LET)) {
            checkAndSkip(KWD_LET::equals);
            return readLet(false);
        } else if (token().equals(KWD_LETREC)) {
            checkAndSkip(KWD_LETREC::equals);
            return readLet(true);
        } else if (token().equals(KWD_CASE)) {
            checkAndSkip(KWD_CASE::equals);
            var expr = readExpression();
            checkAndSkip(KWD_OF::equals);
            checkAndSkip(START_ALT_BLOCK::equals);
            var alts = readAlts();
            return new Case(expr, alts);
        } else {
            return readLitPrimConstrOrApp();
        }
    }

    private boolean isConstructor() {
        return CONS.contains(token());
    }

    private AlgAlt readAlgAlt() throws ParsingFailed {
        var cons = readConstructor(Variable[]::new);
        checkAndSkip(RIGHTARROW::equals);
        var expr = readExpression();
        return new AlgAlt(cons, expr);
    }

    private PrimAlt readPrimAlt() throws ParsingFailed {
        assert (isLiteral());
        var lit = new Literal(tokenAndAdvance());
        checkAndSkip(RIGHTARROW::equals);
        var expr = readExpression();
        return new PrimAlt(lit, expr);
    }

    private Alt[] readAlts() throws ParsingFailed {
        var alts = new ArrayList<Alt>();

        while (!token().equals(END_ALT_BLOCK)) {
            if (isLiteral()) {
                alts.add(readPrimAlt());
            } else if (isConstructor()) {
                alts.add(readAlgAlt());
            } else if (token().equals(KWD_DEFAULT) || isIdentifier()) {
                alts.add(readDefaultAlt());
            } else {
                fail("unrecognized token: " + token());
            }
            checkAndSkipIf(END_OF_BIND::equals);
        }

        checkAndSkip(END_ALT_BLOCK::equals);
        return alts.toArray(Alt[]::new);
    }

    private DefaultAlt readDefaultAlt() throws ParsingFailed {
        var keyToken = tokenAndAdvance();
        checkAndSkip(RIGHTARROW::equals);
        var expr = readExpression();

        return new DefaultAlt(
                Optional.of(keyToken).filter(Predicate.not(KWD_DEFAULT::equals)).map(Variable::ofName),
                expr);
    }

    private Expr readLet(boolean isRec) throws ParsingFailed {
        checkAndSkip(START_LET_BLOCK::equals);
        var binds = new ArrayList<Bind>();
        while (!token().equals(END_LET_BLOCK)) {
            binds.add(readBinding());
            checkAndSkipIf(END_OF_BIND::equals);
        }
        checkAndSkip(END_LET_BLOCK::equals);
        checkAndSkip(KWD_IN::equals);

        var expr = readExpression();
        return new Let(false, binds.toArray(Bind[]::new), expr);
    }

    private Expr readLitPrimConstrOrApp() throws ParsingFailed {
        if (isLiteral()) {
            return new Literal(tokenAndAdvance());
        } else if (PRIM_OPS.contains(token())) {
            var op = tokenAndAdvance();

            var args = readAtomsList();
            return new PrimOp(op, args);
        } else if (CONS.contains(token())) {
            return readConstructor(Atom[]::new);
        } else {
            if (!isIdentifier()) {
                fail("unsupported token application:" + token());
            }
            var var = Variable.ofName(tokenAndAdvance());
            var args = readAtomsList();
            return new Application(var, args);
        }
    }

    private <T> Cons<T> readConstructor(IntFunction<T[]> sup) throws ParsingFailed {
        var cons = tokenAndAdvance();
        var args = readAtomsList();
        return new Cons<T>(cons, Arrays.stream(args).toArray(sup));
    }

    private Atom[] readAtomsList() throws ParsingFailed {
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

    private Atom readAtom() throws ParsingFailed {
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

    private boolean readUpdatabilityFlag() throws ParsingFailed {
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

    private Variable[] readVarBlock() throws ParsingFailed {
        checkAndSkip(START_ATOM_BLOCK::equals);
        var vars = new ArrayList<Variable>();
        while (!token().equals(END_ATOM_BLOCK)) {
            vars.add(readVar());
            checkAndSkipIf(ATOM_SEPARATOR::equals);
        }
        checkAndSkip(END_ATOM_BLOCK::equals);
        return vars.toArray(Variable[]::new);
    }

    private void checkAndSkipIf(Predicate<String> check) {
        if (check.test(token())) {
            advance();
        }
    }

    private Variable readVar() throws ParsingFailed {
        if (!isIdentifier()) {
            fail("invalid variable name: `" + token() + "`");
        }
        return new Variable(tokenAndAdvance());
    }

    private boolean isIdentifier() {
        return token().matches("^[a-zA-Z_][a-zA-Z_0-9]*$") && !CONS.contains(token()) && !KWDS.contains(token());
    }

    private String tokenAndAdvance() {
        var token = token();
        advance();
        return token;
    }

    private void advance() {
        ptr ++;
    }

    private void checkAndSkip(Predicate<String> check) throws ParsingFailed {
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
        return tokens[ptr].text();
    }

    private void fail(String msg) throws ParsingFailed {
        var tokenContext = !isEOF() ? tokens[ptr] : tokens[tokens.length - 1];
        System.err.println(tokenContext.context(20, 20));
        throw new ParsingFailed(msg);
    }

    private boolean isEOF() {
        return ptr >= tokens.length;
    }
}
