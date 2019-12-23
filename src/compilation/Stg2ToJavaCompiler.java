package compilation;

import tea.core.CompileFailed;
import tea.stg2.parser.Bind;
import tea.stg2.parser.expr.Literal;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Stg2ToJavaCompiler {
    private static final String MAIN_CLASS_NAME = "MainAppClass";
    public static final String OFFSET = "    ";

    private boolean debuggingEnabled = true;

    private final Bind[] graph;
    private final Bind main;

    public Stg2ToJavaCompiler(Bind[] graph) {
        this.graph = graph;
        this.main = Arrays.stream(graph).filter(g -> g.var.name.equals("main")).findFirst().orElseThrow();
    }

    private String[] mainClassBody() {
        return block(
                c("class Closure"), block(
                        d("public InfoTable infoPointer = new InfoTable()"),
                        d("public Object[] pointerWords"),      // FIXME typize?
                        d("public Object[] nonPointerWords")),  // FIXME typize?

                c("class InfoTable"), block(
                        d("public CodeLabel standardEntryCode"),
                        d("public CodeLabel evacuationCode"),
                        d("public CodeLabel scavengeCode")
                ),

                c("class Global"), block(
                        d("public Closure MAIN = createMainClosure();")
                ),

                d("private Global global = new Global();"),

                c("interface CodeLabel"), block(
                        d("CodeLabel jumpTo()")
                ),

                m("public CodeLabel MAIN_ENTRY()"), compileMain(),

                m("public CodeLabel MAIN_RETURN_INT()"), block(
                        debug(e(dump("MAIN RETURN integer: \" + ReturnInt +\""))),
                        e("result = new Integer(ReturnInt);"),
                        e("return null;")
                ),

                m("public CodeLabel EVAL_MAIN()"), block(
                        e("Node = global.MAIN;"),
                        e("return ENTER(Node);")
                ),

                e("", "JUMP is implemented via returning of code labels"),
                m("private static CodeLabel ENTER(Closure c)"), block(
                        e("return c.infoPointer.standardEntryCode;")
                ),

                d("private CodeLabel[] A = new CodeLabel[1000]; "), // FIXME implement dynamic allocation and GC
                d("private CodeLabel[] B = new CodeLabel[1000];"), // FIXME implement dynamic allocation

                d("private Closure Node", "Holds the address of the currently evaluating closure"),
                d("private int Sp"),
                d("private int SpA = 0", "front to back indexing"),
                d("private int SpB = B.length-1", "back to front indexing"),

                d("private int ReturnInt", "register to return primitive ints"),
                d("private Object result;", "where we put the final result!"),

                m("public Object eval()"), mainBody(),

                m("public Closure createMainClosure()"), block(
                        e("Closure res = new Closure();"),
                        e("res.infoPointer.standardEntryCode = this::MAIN_ENTRY;"),
                        e("return res;")
                ));
    }

    private String[] compileMain() {
        ensure(main.lf.boundVars.length == 0, "main can't have bound variables");

        String[] source;
        if (main.lf.expr instanceof Literal) {
            source = list(
                    e("ReturnInt = " + ((Literal) main.lf.expr).value + ";", "value to be returned"),
                    e("return this::MAIN_RETURN_INT;"));
        } else {
            throw new CompileFailed("FIXME not implemented!!!");
        }

        return block(
                debug(e(dump("MAIN standard entry."))),
                source);
    }

    private void ensure(boolean b, String s) {
        if (!b) {
            throw new CompilationFailed(s);
        }
    }

    static private String dump(String message) {
        return "System.out.println(\"" + message + "\");";
    }

    private String[] mainBody() {
        return block(
                debug(e("System.out.println(\"Starting execution ...!\");")),

                e("CodeLabel cont = this::EVAL_MAIN;"),

                tinyInterpreterBody(),

                debug(e("System.out.println(\"Execution ended!\");")),
                e("return result;"));
    }

    static private String[] tinyInterpreterBody() {
        return list(
                e("while (cont != null) {"),
                e("    cont = cont.jumpTo();"),
                e("}"));
    }

    private String[] debug(String[] s) {
        return debuggingEnabled ? s : new String[0];
    }

    static private String[] m(String s) {
        return new String[]{s};
    }

    static private String[] e(String s, String... comment) {
        return new String[]{s + (comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", "/*", "*/")))};
    }

    static private String[] c(String classDef) {
        return new String[]{classDef + " "};
    }

    static String[] block(String[]... elems) {
        return Stream.of(
                Stream.of("{"),
                Arrays.stream(elems).flatMap(Arrays::stream).map(s -> OFFSET + s),
                Stream.of("}\n")).flatMap(Function.identity()).toArray(String[]::new);
    }

    static private String[] d(String v, String... comment) {
        return new String[]{v + ";" + (comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", " //", "")))};
    }


    static String[] list(String[]... strs) {
        return Stream.concat(Arrays.stream(strs).flatMap(Arrays::stream), Stream.of("\n")).toArray(String[]::new);
    }

    public String mainClassName() {
        return MAIN_CLASS_NAME;
    }

    public String compile() {
        var lines = list(c("public class " + MAIN_CLASS_NAME), mainClassBody());
        return Arrays.stream(lines).collect(Collectors.joining("\n", "", ""));
    }

}

class CompilationFailed extends RuntimeException {
    public CompilationFailed(String s) {
        super(s);
    }
}
