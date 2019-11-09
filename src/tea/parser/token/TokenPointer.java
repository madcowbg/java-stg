package tea.parser.token;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TokenPointer {
    private final String[] tokens;
    private int ptr;

    public TokenPointer(String[] tokens) {
        this.tokens = tokens;
        this.ptr = 0;
    }

    public Token current() {
        return ptr < tokens.length ? new ValueToken(tokens[ptr]) : Token.EOFToken;
    }

    public void advance() {
        ptr ++;
    }

    @Override
    public String toString() {
        return Stream.of(tokens).limit(ptr).collect(Collectors.joining(" ", "", " ")) +  "<- [ptr=" + ptr + "]";
    }
}
