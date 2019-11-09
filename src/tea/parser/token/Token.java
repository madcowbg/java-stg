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
        public int litteralValue() {
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

        @Override
        public boolean isFunctionHeap() {
            return false;
        }

        @Override
        public boolean isRightArrow() {
            return false;
        }

        @Override
        public boolean isCase() {
            return false;
        }

        @Override
        public boolean isOf() {
            return false;
        }

        @Override
        public boolean isOpenCurly() {
            return false;
        }

        @Override
        public boolean isEndOfAlt() {
            return false;
        }

        @Override
        public boolean isClosedCurly() {
            return false;
        }

        @Override
        public boolean isLitteral() {
            return false;
        }

        @Override
        public boolean isSemicolon() {
            return false;
        }
    };

    boolean isConstructor();

    boolean isKeyword();

    boolean isVariableName();

    boolean isEquals();

    String toString();

    boolean isConstructorHeap();

    int litteralValue();

    boolean isEndBrace();

    boolean isAtom();

    String inner();

    boolean isEndOfBinding();

    boolean isEOF();

    boolean isFunctionHeap();

    boolean isRightArrow();

    boolean isCase();

    boolean isOf();

    boolean isOpenCurly();

    boolean isEndOfAlt();

    boolean isClosedCurly();

    boolean isLitteral();

    boolean isSemicolon();
}
