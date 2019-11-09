package tea.parser.token;

import java.util.Map;
import java.util.function.Function;

public class ValueToken implements Token {

    private static final Map<String, Function> CONSTRUCTORS = Map.of(
            "I", Function.identity()
    );

    private static final Map<String, Function> KEYWORDS = Map.of(
            "CONS", Function.identity()
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
        return !isKeyword() && !isConstructor() && value.matches("^[a-zA-Z_]*$");
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
    public int atomValue() {
        return Integer.parseInt(value);
    }

    @Override
    public boolean isEndBrace() {
        return value.equals(")");
    }

    @Override
    public boolean isAtom() {
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
        return value.equals(";");
    }

    @Override
    public boolean isEOF() {
        return false;
    }
}
