package tea.core;

import tea.core.source.*;
import tea.tokenizer.AnnotatedToken;
import tea.tokenizer.Tokenizer;

import java.util.ArrayList;

/**
 * Core language
 * var v1 v2 ... = e;
 * e -> int | v | (f e1 e2)
 */
public class Parser {

    private static final String EQUALS_SIGN = "=";
    private static final String OPEN_BRACE = "(";
    private static final String CLOSE_BRACE = ")";
    private static final String BIND_SEPARATOR = ";";

    private final AnnotatedToken[] tokens;
    private final Bind[] binds;

    private int ptr;

    public Parser(String[] lines) throws ParsingFailed {
        this.tokens = Tokenizer.tokenizeAnnotated(lines);
        this.ptr = 0;

        var binds = new ArrayList<Bind>();
        while (!isEOF()) {
            var funDef = readFunctionDef();
            checkAndSkip(EQUALS_SIGN);
            var expr = readExpression();
            binds.add(new Bind(funDef, expr));
            checkAndSkip(BIND_SEPARATOR);
        }

        this.binds = binds.toArray(Bind[]::new);
    }

    private Expr readExpression() throws ParsingFailed {
        if (isLiteral()) {
            return readLiteral();
        } else if (token().text().equals(OPEN_BRACE)) {
            checkAndSkip(OPEN_BRACE);
            var f = readExpression();
            var args = new ArrayList<Expr>();
            while (!token().text().equals(CLOSE_BRACE)) {
                args.add(readExpression());
            }
            checkAndSkip(CLOSE_BRACE);
            return new Application(f, args.toArray(Expr[]::new));
        } else {
            return readVariable();
        }
    }

    private Expr readVariable() throws ParsingFailed {
        if (!isIdentifier()) {
            throw new ParsingFailed("needed a variable, got `" + token().text() + "` instead\n" + token().context(20, 20));
        }
        return new Variable(tokenAndAdvance().text());
    }

    private AnnotatedToken tokenAndAdvance() {
        var result = token();
        advance();
        return result;
    }

    private void advance() {
        ptr++;
    }

    private boolean isIdentifier() {
        return token().text().matches("^[a-zA-Z_][a-zA-Z_0-9]*$");
    }

    private Literal readLiteral() {
        return new Literal(Integer.valueOf(tokenAndAdvance().text()));
    }

    private boolean isLiteral() {
        try {
            Integer.valueOf(token().text());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Def readFunctionDef() throws ParsingFailed {
        var f = readIdentifier();
        var args = new ArrayList<Identifier>();
        while (!token().text().equals(EQUALS_SIGN)) {
            args.add(readIdentifier());
        }
        return new Def(f, args.toArray(Identifier[]::new));
    }

    private Identifier readIdentifier() throws ParsingFailed {
        if (!isIdentifier()) {
            throw new ParsingFailed("needed an identifier, got `" + token().text() + "`instead\n" + token().context(20, 20));
        }
        return new Identifier(tokenAndAdvance().text());
    }

    private void checkAndSkip(String expected) throws ParsingFailed {
        if (isEOF()) {
            throw new ParsingFailed("EOF reached instead of `" + expected + "`:\n" + token().context(20, 20));
        }
        if (!token().text().equals(expected)) {
            throw new ParsingFailed("Instead of `" + expected + "` there is `" + token().text() + ":\n" + token().context(20, 20));
        }
        advance();
    }

    private AnnotatedToken token() {
        assert !isEOF();
        return tokens[ptr];
    }

    private boolean isEOF() {
        return ptr >= tokens.length;
    }

    public Bind[] graph() {
        return binds;
    }
}

