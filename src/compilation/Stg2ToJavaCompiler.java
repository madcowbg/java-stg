package compilation;

import tea.core.CompileFailed;
import tea.stg2.parser.Bind;
import tea.stg2.parser.LambdaForm;
import tea.stg2.parser.expr.Application;
import tea.stg2.parser.expr.Literal;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Stg2ToJavaCompiler {
    public static final String ENTRY_POINT = "main";
    private static final String MAIN_CLASS_NAME = "MainAppClass";
    public static final String OFFSET = "    ";

    private boolean debuggingEnabled = true;

    private final String description;

    private final Map<String, Bind> graph;
    private final Map<String, String> globalNames;

    private final HashMap<LambdaForm, String> lfNames = new HashMap<>();
    private int lfIter = 0;

    public Stg2ToJavaCompiler(Bind[] graph, String description) {
        this.description = description;
        this.graph = Arrays.stream(graph).collect(Collectors.toMap(b -> b.var.name, Function.identity()));
        this.globalNames = Arrays.stream(graph).map(b -> b.var.name).collect(Collectors.toMap(Function.identity(), n -> "GLOBAL_" + n));
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
                        d("public Closure MAIN = initGlobalMainClosure()"),
                        flatten(globalNonMainBinds().map(this::globalClosureDeclaration))
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
                        e("B[++SpB] = this::MAIN_RETURN_INT;"),
                        e("Node = global.MAIN;"),
                        e("return ENTER(Node);")
                ),

                flatten(globalNonMainBinds().map(this::globalBindsFunctions)),

                e("", "JUMP is implemented via returning of code labels"),
                m("private static CodeLabel ENTER(Closure c)"), block(
                        e("return c.infoPointer.standardEntryCode;")
                ),

                comment("Stacks"),
                d("private Object[] A = new Object[1000]; "), // FIXME implement dynamic allocation and GC
                d("private CodeLabel[] B = new CodeLabel[1000];"), // FIXME implement dynamic allocation

                comment("Registers"),
                d("private Closure Node", "Holds the address of the currently evaluating closure"),
                d("private int SpA = A.length", "grows backwards"),
                d("private int SpB = -1", "grows forwards"),

                comment("Return Registers"),
                d("private int ReturnInt", "register to return primitive ints"),
                d("private Object result", "where we put the final result!"),

                m("public Object eval()"), mainBody(),

                m("public Closure initGlobalMainClosure()"), block(
                        e("Closure res = new Closure();"),
                        e("res.infoPointer.standardEntryCode = this::MAIN_ENTRY;"),
                        e("return res;")
                ),
                m("private void dumpState()"), block(
                        e(dump("SpA = \" + SpA + \"")),
                        e(dump("A = ...\" + Arrays.toString(IntStream.range(SpA-10, A.length).mapToObj(i -> A[i]).toArray(Object[]::new)) + \"")),
                        e(dump("SpB = \" + SpB + \"")),
                        e(dump("B = \" + Arrays.toString(IntStream.range(0, SpB + 10).mapToObj(i -> B[i]).toArray(Object[]::new)) + \"")),
                        e(dump("Node = \" + Node + \""))
                ));
    }

    private String[] globalBindsFunctions(Bind bind) {
        return list(
                m("public Closure " + globalClosureInitName(bind) + "()"), block(
                        e("Closure res = new Closure();"),
                        e("res.infoPointer.standardEntryCode = this::" + lfEntry(bind.lf) + ";"),
                        e("return res;")
                ),

                m("public CodeLabel " + lfEntry(bind.lf) + "()", "ENTRY OF:", bind.lf.toString()), block(
                        lambdaFormEntryBlock(bind.lf)
                )
        );
    }

    private String[] lambdaFormEntryBlock(LambdaForm lf) {
        List<String> source = new ArrayList<>();

        // fixme use common buildBlock method
        var variableIdxs = IntStream.range(0, lf.boundVars.length).boxed()
                .collect(Collectors.toMap(i -> lf.boundVars[i], Function.identity()));
        var variableNames = Arrays.stream(lf.boundVars).collect(
                Collectors.toMap(Function.identity(), name -> "LOCAL_" + name));

        // TODO argument satisfaction check
        // TODO stack overflow check
        // TODO heap overflow check
        // pop bound vars
        source.addAll(List.of(list(
                variableIdxs.entrySet().stream()
                        .map(e -> e("Object " + variableNames.get(e.getKey()) + " = A[SpA + " + e.getValue() + "];"))
                        .flatMap(Arrays::stream).toArray(String[]::new),
                e("SpA = SpA + " + (variableIdxs.size()) + ";", "adjust popped stack all!")
        )));

        if (lf.expr instanceof Literal) {
            source.addAll(List.of(list(
                    e("ReturnInt = " + ((Literal) lf.expr).value + ";", "value to be returned"),
                    e("return B[SpB--];"))));
        } else if (lf.expr instanceof Application) {
            var call = ((Application)lf.expr);

            for (int i = 0; i < call.args.length; i++) {
                if (call.args[i] instanceof Literal) {
                    var lit = (Literal)call.args[i];
                    source.addAll(List.of(
                            e("A[--SpA] = " + lit.value + ";", "add arguments")));
                } else {
                    throw new CompileFailed("FIXME not implemented arguments that are not literals!!!");
                }
            }

            if (globalNames.get(call.f.name) == null) {
                throw new CompileFailed("FIXME not implemented!!!");
            }
            // call to non-built-in function
            source.addAll(List.of(list(
                    e("Node = global." + globalNames.get(call.f.name) + ";", "enter global closure"),
                    e("return ENTER(Node);")
            )));

        } else {
            throw new CompileFailed("FIXME not implemented!!!");
        }

        return source.toArray(String[]::new);
    }

    private String lfEntry(LambdaForm lf) {
        return lfNames.computeIfAbsent(lf, l -> "LF_" + (lfIter++) + "_ENTRY");
    }

    private String[] flatten(Stream<String[]> vals) {
        return  vals.flatMap(Arrays::stream).toArray(String[]::new);
    }

    private Stream<Bind> globalNonMainBinds() {
        return graph.values().stream()
                .filter(b -> !b.var.name.equals(ENTRY_POINT));
    }

    private String[] globalClosureDeclaration(Bind bind) {
        return d("public Closure " + globalNames.get(bind.var.name) + " = " + globalClosureInitName(bind) + "();");
    }

    private String globalClosureInitName(Bind bind) {
        return "init" + globalNames.get(bind.var.name) + "Closure";
    }

    private String[] compileMain() {
        Bind main = graph.get("main");

        ensure(main.lf.boundVars.length == 0, "main can't have bound variables");

        var source = lambdaFormEntryBlock(main.lf);

        return block(
                debug(e(dump("MAIN standard lfEntry."))),
                source);
    }

    private void ensure(boolean b, String s) {
        if (!b) {
            throw new CompilationFailed(s);
        }
    }

    static private String dump(String message) {
        return "System.err.println(\"" + message + "\");";
    }

    private String[] mainBody() {
        return block(
                debug(e(dump("Starting execution ...!"))),

                e("CodeLabel cont = this::EVAL_MAIN;"),

                e("try"), block(
                        tinyInterpreterBody()
                ), e("catch (RuntimeException e) "), block(
                        debug(e(dump("========== Exception raised!"))),
                        e("dumpState();"),
                        e("e.printStackTrace();"),
                        e("throw new RuntimeException(e);")
                ),
                debug(e(dump("Execution ended!"))),
                e("return result;"));
    }

    private String[] tinyInterpreterBody() {
        return list(
                e("while (cont != null) {"),
                debug(e(dump("========== Dumping state before jump... ============"))),
                debug(e("dumpState();")),
                debug(e(dump("============ Jumping to: \" + cont.toString() + \""))),
                e("    cont = cont.jumpTo();"),
                e("}"));
    }

    private String[] debug(String[] s) {
        return debuggingEnabled ? s : new String[0];
    }

    static private String[] m(String s, String... comments) {
        return comments.length == 0 ?
                new String[]{"", s} :
                Stream.of(
                        Stream.of("", "/* "),
                        Arrays.stream(comments),
                        Stream.of(" */"),
                        Stream.of(s)).flatMap(Function.identity()).toArray(String[]::new);
    }

    static private String[] e(String s, String... comment) {
        return new String[]{s + (comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", "/* ", " */")))};
    }

    static private String[] c(String classDef) {
        return new String[]{"", classDef + " "};
    }

    static String[] block(String[]... elems) {
        return Stream.of(
                Stream.of("{"),
                Arrays.stream(elems).flatMap(Arrays::stream).map(s -> OFFSET + s),
                Stream.of("}")).flatMap(Function.identity()).toArray(String[]::new);
    }

    static private String[] d(String v, String... comment) {
        return new String[]{v + ";" + (comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", " //", "")))};
    }


    static String[] list(String[]... strs) {
        return Arrays.stream(strs).flatMap(Arrays::stream).toArray(String[]::new);
    }

    public String mainClassName() {
        return MAIN_CLASS_NAME;
    }

    private String[] toLines() {
        return list(
                e("import java.util.*;"),
                e("import java.util.stream.IntStream;"),
                comment(description),
                c("public class " + MAIN_CLASS_NAME), mainClassBody()
        );
    }

    private static String[] comment(String text) {
        return new String[]{"/* ", text, " */"};
    }

    public String compile() {
        var lines = toLines();
        return Arrays.stream(lines).collect(Collectors.joining("\n", "", ""));
    }

    public String withLNs() {
        var lines = compile().split("\n");
        return IntStream.range(0, lines.length).mapToObj(i -> String.format("%03d\t%s", i+1, lines[i])).collect(Collectors.joining("\n", "", ""));
    }
}

class CompilationFailed extends RuntimeException {
    public CompilationFailed(String s) {
        super(s);
    }
}
