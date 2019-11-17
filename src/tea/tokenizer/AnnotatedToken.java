package tea.tokenizer;

public class AnnotatedToken {
    private final String text;
    private final int ptr;
    private final String original;

    public AnnotatedToken(String text, int ptr, String original) {
        this.text = text;
        this.ptr = ptr;
        this.original = original;
    }

    public String text() {
        return text;
    }
}
