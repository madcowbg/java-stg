package compilation;

import tea.stg2.parser.Bind;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Stg2ToJavaCompiler extends SourceBuilder {
    private static final String MAIN_CLASS_NAME = "MainAppClass";
    public static final String OFFSET = "    ";

    private boolean debuggingEnabled = true;

    public Stg2ToJavaCompiler(Bind[] graph) {
        write(
                c("public class " + MAIN_CLASS_NAME), mainClassBody());

    }

    private String[] mainClassBody() {
        return block(
                c("static class ClosureData"), block(
                        d("public InfoTable infoPointer"),
                        d("public Object[] pointerWords"),      // FIXME typize?
                        d("public Object[] nonPointerWords")),  // FIXME typize?

                c("static class InfoTable"), block(
                        d("public CodeLabel standardEntryCode"),
                        d("public CodeLabel evacuationCode"),
                        d("public CodeLabel scavengeCode")
                ),

                c("interface CodeLabel"), block(
                        d("CodeLabel jumpTo()")
                ),

                d("private static int Node", "Holds the address of the currently evaluating closure"),
                d("private static int Sp"),
                d("private static int SpA"),
                d("private static int SpB"),

                f("public static void main(String[] var0)"), tinyInterpreterBody());
    }

    private String[] tinyInterpreterBody() {
        return block(
                debug(e("System.out.println(\"Starting execution ...!\");")),

                e("CodeLabel cont = () -> {"),
                e("    return null;"),
                e("};"),

                e("while (cont != null) {"),
                e("    cont = cont.jumpTo();"),
                e("}"),
                debug(e("System.out.println(\"Execution ended!\");")));
    }

    private String[] debug(String[] s) {
        return debuggingEnabled ? s : new String[0];
    }

    private String[] f(String s) {
        return new String[]{s};
    }

    private String[] e(String s) {
        return new String[]{s};
    }

    private String[] c(String classDef) {
        return new String[]{classDef + " "};
    }

    private void write(String[]... strs) {
        for (var str : strs) {
            source.append(Arrays.stream(str).collect(Collectors.joining("\n", "", "")));
        }
    }

    String[] block(String[]... elems) {
        return Stream.of(
                Stream.of("{"),
                Arrays.stream(elems).flatMap(Arrays::stream).map(s -> OFFSET + s),
                Stream.of("}\n")).flatMap(Function.identity()).toArray(String[]::new);
    }

    private String[] d(String v, String... comment) {
        return new String[]{v + ";" + (comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", " //", "")))};
    }


    String[] list(String[]... strs) {
        return Stream.concat(Arrays.stream(strs).flatMap(Arrays::stream), Stream.of("\n")).toArray(String[]::new);
    }

    public String mainClassName() {
        return MAIN_CLASS_NAME;
    }
}
