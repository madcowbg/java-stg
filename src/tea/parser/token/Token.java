package tea.parser.token;

import java.util.Map;
import java.util.function.Function;

public class Token {

    private static final Map<String, Function> CONSTRUCTORS = Map.of(
            "I", Function.identity()
    );

    private static final Map<String, Function> KEYWORDS = Map.of(
            "CONS(", Function.identity(),
            "FUN(", Function.identity(),
            "case", Function.identity()
    );

    public final String text;

    Token(String text) {
        this.text = text;
    }

    public boolean isConstructor() {
        return Token.CONSTRUCTORS.containsKey(text);
    }

    public boolean isKeyword() {
        return Token.KEYWORDS.containsKey(text);
    }

    public boolean isVariableName() {
        return !isKeyword() && !isConstructor() && text.matches("^[a-zA-Z_][a-zA-Z_0-9]*$");
    }

    public String toString() {
        return "T[" + text + "]";
    }


    public int litteralValue() {
        return Integer.parseInt(text);
    }

    public boolean isAtom() {
        return isVariableName() || isLitteral();
    }

    public boolean isLitteral() {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
