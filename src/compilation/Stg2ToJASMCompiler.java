package compilation;

import tea.stg2.parser.Bind;

public class Stg2ToJASMCompiler extends SourceBuilder {
    private static final String MAIN_CLASS = "MainClassOfApp";

    public Stg2ToJASMCompiler(Bind[] graph) {
        super(" ; ");
        header();

        src("    .limit stack 2", "up to two items can be pushed");
        src("    .limit locals 2");

        // TODO complete implementation

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

    public String mainClassName() {
        return MAIN_CLASS;
    }
}
