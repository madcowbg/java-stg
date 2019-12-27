package tea.stg1.parser.token;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tea.stg1.parser.token.Token.PRIMOPS;

public class TokenPointer {
    private static final Predicate<Token> ALWAYS_FAIL = (a) -> false;

    private final Token[] tokens;
    private int ptr;

    public TokenPointer(String[] tokens) {
        this.tokens = Arrays.stream(tokens).map(Token::new).toArray(Token[]::new);
        this.ptr = 0;
    }

    private boolean checkToken(Predicate<Token> check) {
        return !isEOF() && check.test(token());
    }

    private Token token() {
        assert !isEOF();
        return tokens[ptr];
    }

    public void advance() {
        ptr ++;
    }

    public void skipAfterCheck(Predicate<Token> check, String msgOnFail) {
        checkCurrent(check, msgOnFail);
        advance();
    }

    public void skipAfterCheck(String skip) {
        skipAfterCheck(t -> t.text.equals(skip), ", but expected " + skip);
    }

    @Override
    public String toString() {
        return Stream.of(tokens).limit(ptr).map(t -> t.text).collect(Collectors.joining(" ", "", " ")) +  "<- [ptr=" + ptr + "]";
    }

    public Token advanceAfterCheck(Predicate<Token> check, String msgOnFail) {
        if (isEOF()) {
            throw new RuntimeException("EOF reached while still requiring stuff...");
        }

        var token = token();
        skipAfterCheck(check, msgOnFail);

        return token;
    }

    public void checkCurrent(Predicate<Token> check, String msgOnFail) {
        if (isEOF()) {
            throw new RuntimeException(this.toString() + " ! EOF reached...");
        } else if (!check.test(token())) {
            throw new RuntimeException(token() + msgOnFail + "\n" + this);
        }
    }

    public void skipIf(String skip) {
        if (!isEOF() && token().text.equals(skip)) {
            advance();
        }
    }

    public void checkCurrent(String skip) {
        checkCurrent(t -> t.text.equals(skip), ", but expected " + skip);
    }

    public void fail(String msg) {
        checkCurrent(ALWAYS_FAIL, msg);
    }

    public boolean isEOF() {
        return ptr >= tokens.length;
    }

    public boolean is(String text) {
        return checkToken(t -> t.text.equals(text));
    }

    public boolean isAtom() {
        return checkToken(Token::isAtom);
    }

    public boolean isVariableName() {
        return checkToken(Token::isVariableName);
    }

    public boolean isLitteral() {
        return checkToken(Token::isLitteral);
    }

    public boolean isConstructor() {
        return checkToken(Token::isConstructor);
    }

    public boolean isPrimOp() {
        return checkToken(t -> PRIMOPS.containsKey(t.text));
    }
}
