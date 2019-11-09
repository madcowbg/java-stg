package tea.parser.token;

import java.util.Map;
import java.util.function.Function;

public class ValueToken implements Token {

    private static final Map<String, Function> CONSTRUCTORS = Map.of(
            "I", Function.identity()
    );

    private static final Map<String, Function> KEYWORDS = Map.of(
            "CONS(", Function.identity(),
            "FUN(", Function.identity(),
            "case", Function.identity()
    );

    private final String value;


    ValueToken(String value) {
        this.value = value;
    }

    @Override
    public boolean isConstructor() {
        return CONSTRUCTORS.containsKey(value);
    }

    @Override
    public boolean isKeyword() {
        return KEYWORDS.containsKey(value);
    }

    @Override
    public boolean isVariableName() {
        return !isKeyword() && !isConstructor() && value.matches("^[a-zA-Z_][a-zA-Z_0-9]*$");
    }

    @Override
    public boolean isEquals() {
        return value.equals("=");
    }

    @Override
    public String toString() {
        return "T[" + value + "]";
    }

    @Override
    public boolean isConstructorHeap() {
        return value.equals("CON(");
    }

    @Override
    public int litteralValue() {
        return Integer.parseInt(value);
    }

    @Override
    public boolean isEndBrace() {
        return value.equals(")");
    }

    @Override
    public boolean isAtom() {
        return isVariableName() || isLitteral();

    }

    @Override
    public boolean isLitteral() {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public String inner() {
        return value;
    }

    @Override
    public boolean isEndOfBinding() {
        return isSemicolon();
    }

    @Override
    public boolean isSemicolon() {
        return value.equals(";");
    }

    @Override
    public boolean isEOF() {
        return false;
    }

    @Override
    public boolean isFunctionHeap() {
        return value.equals("FUN(");
    }

    @Override
    public boolean isRightArrow() {
        return value.equals("->");
    }

    @Override
    public boolean isCase() {
        return value.equals("case");
    }

    @Override
    public boolean isOf() {
        return value.equals("of");
    }

    @Override
    public boolean isOpenCurly() {
        return value.equals("{");
    }

    @Override
    public boolean isEndOfAlt() {
        return isSemicolon() || isClosedCurly();
    }

    @Override
    public boolean isClosedCurly() {
        return value.equals("}");
    }
}
