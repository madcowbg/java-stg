package tea.tokenizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Tokenizer {
    public static String[] tokenize(String[] lines) {
        return Arrays.stream(tokenizeAnnotated(lines)).map(AnnotatedToken::text).toArray(String[]::new);
    }

    public static AnnotatedToken[] tokenizeAnnotated(String[] lines) {
        return new Source(lines).tokens();
    }
}

class Source {
    private final String[] lines;
    private final String[] sourceLines;
    private final AnnotatedToken[] tokens;

    public Source(String[] lines) {
        this.lines = lines;

        this.sourceLines = Stream.of(lines)
                .filter(Predicate.not(String::isBlank))
                .map(String::trim)
                .filter(Predicate.not(Source::isComment))
                .toArray(String[]::new);

        this.tokens = new AnnotatedString(String.join(" \n", sourceLines)) // assume space at end of lines
                .replace(";", " ; ") // simplify semicolon tokenization
                .replace("(", "( ")
                .replace(")", " ) ")
                .replace("{", " { ")
                .replace("}", " } ")
                .replace("=", " = ")
                .replace(",", " , ")
                .replace("\t", " ") // simplify whitespaces
                .replaceAll("\\s{2,}", " ") // cleanup whitespaces
                .split("\\s");
    }

    public AnnotatedToken[] tokens() {
        return tokens;
    }

    static boolean isComment(String s) {
        return s.startsWith("#");
    }

}

class AnnotatedString {
    private final String original;

    private final String text;
    private final int[] ptrs;

    public AnnotatedString(String text) {
        this(text, IntStream.range(0, text.length()).toArray(), text);
    }

    public AnnotatedString(String text, int[] ptrs, String original) {
        assert ptrs.length == text.length();
        assert Arrays.stream(ptrs).max().orElse(Integer.MAX_VALUE) <= original.length();

        this.text = text;
        this.original = original;
        this.ptrs = ptrs;
    }

    public AnnotatedString replace(String of, String to) {
        StringBuilder sb = new StringBuilder();
        var ptrs = new ArrayList<Integer>();

        int fromIndex = 0, nextIdx;
        while ((nextIdx = text.indexOf(of, fromIndex)) != -1) {
            // copy text before match
            sb.append(text, fromIndex, nextIdx);
            for (int i = fromIndex; i < nextIdx; i++) {
                ptrs.add(this.ptrs[i]);
            }

            // copy matched string
            sb.append(to);
            for (int i = 0; i < to.length(); i++) {
                ptrs.add(this.ptrs[nextIdx - 1]);
            }
            fromIndex = nextIdx + of.length();
        }
        sb.append(text, fromIndex, text.length());
        for (int i = fromIndex; i < text.length(); i++) {
            ptrs.add(this.ptrs[i]);
        }

        return new AnnotatedString(sb.toString(), ptrs.stream().mapToInt(Integer::intValue).toArray(), original);
    }

    public AnnotatedString replaceAll(String ofRegex, String to) {
        var matcher = Pattern.compile(ofRegex).matcher(text);

        StringBuilder sb = new StringBuilder();
        var ptrs = new ArrayList<Integer>();
        int currIdx = 0, nextIdx;
        while (matcher.find()) {
            nextIdx = matcher.start();

            // append before ...
            sb.append(text, currIdx, nextIdx);
            for (int i = currIdx; i < nextIdx; i++) {
                ptrs.add(this.ptrs[i]);
            }

            // append replacement
            sb.append(to);
            for (int i = 0; i < to.length(); i++) {
                ptrs.add(this.ptrs[nextIdx - 1]);
            }

            currIdx = matcher.end();
        }
        sb.append(text, currIdx, text.length());
        for (int i = currIdx; i < text.length(); i++) {
            ptrs.add(this.ptrs[i]);
        }

        return new AnnotatedString(sb.toString(), ptrs.stream().mapToInt(Integer::intValue).toArray(), original);
    }

    public AnnotatedToken[] split(String regex) {
        var tokens = new ArrayList<AnnotatedToken>();
        int currentPos = 0, nextPos;
        var matcher = Pattern.compile(regex).matcher(text);
        while (matcher.find()) {
            nextPos = matcher.start();
            tokens.add(new AnnotatedToken(text.substring(currentPos, nextPos), ptrs[currentPos], original));
            currentPos = matcher.end();
        }

        if (currentPos < text.length()) {
            tokens.add(new AnnotatedToken(text.substring(currentPos), ptrs[currentPos], original));
        }

        return tokens.toArray(AnnotatedToken[]::new);
    }

}

