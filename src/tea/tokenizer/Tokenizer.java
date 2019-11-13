package tea.tokenizer;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tokenizer {
    public static String[] tokenize(String[] lines) {
        return Stream.of(lines)
                .filter(Predicate.not(String::isBlank))
                .map(String::trim)
                .filter(Predicate.not(Tokenizer::isComment))
                .collect(Collectors.joining(" ")) // assume space at end of lines
                .replace(";", " ; ") // simplify semicolon tokenization
                .replace("(", "( ")
                .replace(")", " ) ")
                .replace("{", " { ")
                .replace("}", " } ")
                .replace("=", " = ")
                .replace('\t', ' ') // simplify whitespaces
                .replaceAll("\\s{2,}", " ") // cleanup whitespaces
                .split("\\s");
    }

    private static boolean isComment(String s) {
        return s.startsWith("#");
    }
}
