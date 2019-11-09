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

    final String value;

    Token(String value) {
        this.value = value;
    }

    public boolean isConstructor() {
        return Token.CONSTRUCTORS.containsKey(value);
    }

    public boolean isKeyword() {
        return Token.KEYWORDS.containsKey(value);
    }

    public boolean isVariableName() {
        return !isKeyword() && !isConstructor() && value.matches("^[a-zA-Z_][a-zA-Z_0-9]*$");
    }

    public boolean isEquals() {
        return value.equals("=");
    }

    public String toString() {
        return "T[" + value + "]";
    }

    public boolean isConstructorHeap() {
        return value.equals("CON(");
    }

    public int litteralValue() {
        return Integer.parseInt(value);
    }

    public boolean isEndBrace() {
        return value.equals(")");
    }

    public boolean isAtom() {
        return isVariableName() || isLitteral();

    }

    public boolean isLitteral() {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String inner() {
        return value;
    }

    public boolean isEndOfBinding() {
        return isSemicolon();
    }

    public boolean isSemicolon() {
        return value.equals(";");
    }

    public boolean isFunctionHeap() {
        return value.equals("FUN(");
    }

    public boolean isRightArrow() {
        return value.equals("->");
    }

    public boolean isCase() {
        return value.equals("case");
    }

    public boolean isOf() {
        return value.equals("of");
    }

    public boolean isOpenCurly() {
        return value.equals("{");
    }

    public boolean isEndOfAlt() {
        return isSemicolon() || isClosedCurly();
    }

    public boolean isClosedCurly() {
        return value.equals("}");
    }
}
