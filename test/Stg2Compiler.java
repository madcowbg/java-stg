import tea.stg2.parser.Bind;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Stg2Compiler {
    private static final String MAIN_CLASS = "MainClassOfApp";
    private final StringBuilder source = new StringBuilder();

    public Stg2Compiler(Bind[] graph) {
        header();

        src("    .limit stack 2", "up to two items can be pushed");
        src("    .limit locals 2");

        footer();
    }

    private void header() {
        src(".class public " + mainClassName());
        src(".super java/lang/Object");

        comment("");
        comment("standard initializer (calls java.lang.Object's initializer)");
        comment("");
        src(".method public <init>()V");
        src("    aload_0");
        src("    invokenonvirtual java/lang/Object/<init>()V");
        src("    return");
        src(".end method");
        src("");

        comment(";");
        comment("; main() - entry point of the class");
        comment(";");
        src(".method public static main([Ljava/lang/String;)V");
    }

    private void footer() {
        comment("    ; done");
        src("    return");
        src(".end method");
    }

    private void comment(String... cs) {
        for(var c: cs) {
            src(" ; " + c);
        }
    }

    private void src(String l, String... comment) {
        assert !l.contains("\n");
        source.append(l);
        if (comment.length > 0) {
            source.append(Arrays.stream(comment).collect(Collectors.joining(";", " ;", "")));
        }
        source.append("\n");
    }

    public String mainClassName() {
        return MAIN_CLASS;
    }

    public byte[] byteArray() {
        return source.toString().getBytes();
    }
}
