package tea.parser.token;

import java.util.Map;
import java.util.function.Function;

public class Token {

    private static final Map<String, Function> CONSTRUCTORS = Map.of(
            "I", Function.identity()
    );

    public static final String KWD_CON = "CON(";
    public static final String KWD_FUN = "FUN(";
    public static final String KWD_CASE = "case";
    public static final String KWD_OF = "of";
    public static final String KWD_THUNK = "THUNK(";
    public static final String KWD_LET = "let";
    public static final String KWD_IN = "in";

    private static final Map<String, Function> KEYWORDS = Map.of(
            KWD_CON, Function.identity(),
            KWD_FUN, Function.identity(),
            KWD_CASE, Function.identity(),
            KWD_OF, Function.identity(),
            KWD_THUNK, Function.identity(),
            KWD_LET, Function.identity(),
            KWD_IN, Function.identity()
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Token && ((Token) obj).text.equals(text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }
}
