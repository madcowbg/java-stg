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

    public String context(int offsetBefore, int offsetAfter) {
        var from = Math.max(0, ptr - offsetBefore);
        var to = Math.min(ptr + offsetAfter, original.length());
        var before = ptr - from;
        var after = to - ptr;
        return original.substring(from, to).replace("\n", "¶") + "\n"
            + " ".repeat(before) + "^" + " ".repeat(after);
    }
}
