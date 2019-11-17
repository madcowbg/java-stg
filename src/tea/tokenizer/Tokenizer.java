package tea.tokenizer;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tokenizer {
    public static String[] tokenize(String[] lines) {
        return new Source(lines).tokens();
    }
}

class Source {
    private final String[] lines;
    private final String[] sourceLines;
    private final String[] tokens;

    public Source(String[] lines) {
        this.lines = lines;

        this.sourceLines = Stream.of(lines)
                .filter(Predicate.not(String::isBlank))
                .map(String::trim)
                .filter(Predicate.not(Source::isComment))
                .toArray(String[]::new);

        this.tokens = String.join(" \n", sourceLines) // assume space at end of lines
                .replace(";", " ; ") // simplify semicolon tokenization
                .replace("(", "( ")
                .replace(")", " ) ")
                .replace("{", " { ")
                .replace("}", " } ")
                .replace("=", " = ")
                .replace(",", " , ")
                .replace('\t', ' ') // simplify whitespaces
                .replaceAll("\\s{2,}", " ") // cleanup whitespaces
                .split("\\s");
    }

    public String[] tokens() {
        return tokens;
    }

    static boolean isComment(String s) {
        return s.startsWith("#");
    }
}
