package tea.parser.token;

public interface Token {
    Token EOFToken = new Token() {
        @Override
        public boolean isConstructor() {
            return false;
        }

        @Override
        public boolean isKeyword() {
            return false;
        }

        @Override
        public boolean isVariableName() {
            return false;
        }

        @Override
        public boolean isEquals() {
            return false;
        }

        @Override
        public String toString() {
            return "!!!EOF!!!";
        }

        @Override
        public boolean isConstructorHeap() {
            return false;
        }

        @Override
        public int atomValue() {
            throw new UnsupportedOperationException("can't get value of EOF token!");
        }

        @Override
        public boolean isEndBrace() {
            return false;
        }

        @Override
        public boolean isAtom() {
            return false;
        }

        @Override
        public String inner() {
            throw new UnsupportedOperationException("can't get value of EOF token!");
        }

        @Override
        public boolean isEndOfBinding() {
            return true;
        }

        @Override
        public boolean isEOF() {
            return true;
        }
    };

    boolean isConstructor();

    boolean isKeyword();

    boolean isVariableName();

    boolean isEquals();

    String toString();

    boolean isConstructorHeap();

    int atomValue();

    boolean isEndBrace();

    boolean isAtom();

    String inner();

    boolean isEndOfBinding();

    boolean isEOF();
}
