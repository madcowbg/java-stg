package compilation;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SourceBuilder {
    protected final StringBuilder source = new StringBuilder();
    private final String commentSeparator;

    public SourceBuilder(String commentSeparator) {
        this.commentSeparator = commentSeparator;
    }

    protected void comment(String... cs) {
        for(var c: cs) {
            src(commentSeparator + c);
        }
    }

    protected void src(String l, String... comment) {
        assert !l.contains("\n");
        source.append(l);
        if (comment.length > 0) {
            source.append(Arrays.stream(comment).collect(Collectors.joining(commentSeparator, commentSeparator, "")));
        }
        source.append("\n");
    }

    public byte[] byteArray() {
        return source.toString().getBytes();
    }
}
