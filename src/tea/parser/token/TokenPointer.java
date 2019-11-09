package tea.parser.token;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Override
    public String toString() {
        return Stream.of(tokens).limit(ptr).map(t -> t.value).collect(Collectors.joining(" ", "", " ")) +  "<- [ptr=" + ptr + "]";
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
            throw new RuntimeException(token() + msgOnFail);
        }
    }

    public void fail(String msg) {
        checkCurrent(ALWAYS_FAIL, msg);
    }

    public boolean isAtom() {
        return checkToken(Token::isAtom);
    }

    public boolean isCase() {
        return checkToken(Token::isCase);
    }

    public boolean isVariableName() {
        return checkToken(Token::isVariableName);
    }

    public boolean isEndBrace() {
        return checkToken(Token::isEndBrace);
    }

    public boolean isLitteral() {
        return checkToken(Token::isLitteral);
    }

    public boolean isEOF() {
        return ptr >= tokens.length;
    }

    public boolean isConstructorHeap() {
        return checkToken(Token::isConstructorHeap);
    }

    public boolean isFunctionHeap() {
        return checkToken(Token::isFunctionHeap);
    }

    public boolean isRightArrow() {
        return checkToken(Token::isRightArrow);
    }

    public boolean isClosedCurly() {
        return checkToken(Token::isClosedCurly);
    }

    public boolean isSemicolon() {
        return checkToken(Token::isSemicolon);
    }

    public boolean isConstructor() {
        return checkToken(Token::isConstructor);
    }
}
