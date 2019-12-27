package compilation;

import tea.core.CompileFailed;
import tea.stg2.parser.Bind;
import tea.stg2.parser.LambdaForm;
import tea.stg2.parser.alt.AlgAlt;
import tea.stg2.parser.alt.DefaultAlt;
import tea.stg2.parser.alt.PrimAlt;
import tea.stg2.parser.expr.*;

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
    private final Map<String, String> globalNamesInfo;

    private final HashMap<LambdaForm, String> lfNames = new HashMap<>();
    private int lfIter = 0;

    private final List<InnerDec> inners = new ArrayList<>();
    private int nextInner = 0;
    private final List<CompiledCaseCont> continuations = new ArrayList<>();
    private int nextCont = 0;

    /* constructor to class */
    private Map<String, List<String>> classConstructors = Map.of("BoxedInt", List.of("MkInt")); // TODO these could be declared...
    private Map<String, String> constructorClass = classConstructors.entrySet().stream()
            .map(e -> e.getValue().stream().collect(Collectors.toMap(v -> v, v -> e.getKey())))
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private String src = null;

    public Stg2ToJavaCompiler(Bind[] graph, String description) {
        this.description = description;
        this.graph = Arrays.stream(graph).collect(Collectors.toMap(b -> b.var.name, Function.identity()));
        this.globalNames = Arrays.stream(graph).map(b -> b.var.name).collect(Collectors.toMap(Function.identity(), n -> "$" + n + "$_closure"));
        this.globalNamesInfo = Arrays.stream(graph).map(b -> b.var.name).collect(Collectors.toMap(Function.identity(), n -> "$" + n + "$_info"));
    }

    private String[] mainClassBody() {
        return block(
                c("class Closure"), block(
                        d("public final InfoTable infoPointer"),
                        d("public Object[] pointerWords"),      // FIXME typize?
                        d("public Object[] nonPointerWords"),  // FIXME typize?
                        m("Closure(InfoTable infoPointer)"), block(e("this.infoPointer = infoPointer;")),
                        m("Closure(InfoTable infoPointer, Object[] pointerWords, Object[] nonPointerWords)"), block(
                                e("this.infoPointer = infoPointer;"),
                                e("this.pointerWords = pointerWords;"),
                                e("this.nonPointerWords = nonPointerWords;"))),

                c("class InfoTable"), block(
                        d("public final CodeLabel standardEntryCode"),
                        d("public CodeLabel evacuationCode"),
                        d("public CodeLabel scavengeCode"),
                        m("InfoTable(CodeLabel standardEntryCode)"), block(e("this.standardEntryCode = standardEntryCode;"))),

                comment("Global infos and closures"),
                d("public InfoTable MAIN_info = new InfoTable(this::MAIN_entry)"),
                flatten(globalNonMainBinds().map(bind ->
                d("public InfoTable " + globalNamesInfo.get(bind.var.name) + " = new InfoTable(this::"+ lfEntryCode(bind.lf) + ")"))),

                d("public Closure MAIN_closure = new Closure(MAIN_info)"),
                flatten(globalNonMainBinds().map(bind ->
                d("public Closure " + globalNames.get(bind.var.name) + " = new Closure(" + globalNamesInfo.get(bind.var.name) + ")"))),

                comment("Constructors"),
                flatten(constructorClass.keySet().stream().map(this::declareConstructor)), // declare all constructors

                m("public CodeLabel cons_MkInt_entry()"), block(
                        doOnShortB(list(
                                e("result = new Object[]{\"MkInt\", Node.nonPointerWords[0]};", "argument in first non-pointer"),
                                e("return null;")
                        )),
                        e("final CodeLabel[] conts = (CodeLabel[]) B[SpB--];", "pop continuation vector"),
                        e("return conts[0];", "single constructor, so we know it is the first one!")
                ),

                c("interface CodeLabel", "Represents pieces of code"), block(
                        d("CodeLabel jumpTo()")
                ),

                m("public CodeLabel MAIN_entry()"), compileMain(),

                m("public CodeLabel MAIN_RETURN_INT()"), block(
                        debug(e(dump("MAIN RETURN integer: \" + ReturnInt +\""))),
                        e("result = new Integer(ReturnInt);"),
                        e("return null;")
                ),

                m("public CodeLabel EVAL_MAIN()"), block(
                        e("Node = MAIN_closure;"),
                        e("return ENTER(Node);")
                ),

                flatten(globalNonMainBinds().map(this::globalBindsFunctions)),

                m("private static CodeLabel ENTER(Closure c)", "JUMP is implemented via returning of code labels"), block(
                        e("return c.infoPointer.standardEntryCode;")
                ),

                comment("Stacks"),
                d("private Object[] A = new Object[1000]; "), // FIXME implement dynamic allocation and GC
                d("private Object[] B = new Object[1000];"), // FIXME implement dynamic allocation
                d("private Closure[] H = new Closure[1000];"), // FIXME implement dynamic allocation

                comment("Registers"),
                d("private Closure Node", "Holds the address of the currently evaluating closure"),
                d("private int SpA = A.length", "grows backwards"),
                d("private int SpB = -1", "grows forwards"),
                d("private int Hp = -1", "Heap, grows forward..."),

                comment("Return Registers"),
                d("private int ReturnInt", "register to return primitive ints"),
                d("private Object result", "where we put the final result!"),

                m("public Object eval()"), mainBody(),

                m("private void dumpState()"), block(
                        dumpA(),
                        e(dump("SpB = \" + SpB + \"")),
                        e(dump("B = \" + Arrays.toString(IntStream.range(0, SpB + 10).mapToObj(i -> B[i]).toArray(Object[]::new)) + \"")),
                        e(dump("Node = \" + Node + \"")),
                        e(dump("Hp = \" + Hp + \"")),
                        e(dump("H = \" + Arrays.toString(IntStream.range(0, Hp + 2).mapToObj(i -> H[i]).toArray(Object[]::new)) + \"")
                )),

                comment("Inner infos and codes"),
                flatten(inners.stream().map(this::writeInfoAndCode)),

                comment("Continuation codes"),
                flatten(continuations.stream().map(this::writeContinuationSource)),

                m("public static void main(String[] args)"), block(
                        e("new " + MAIN_CLASS_NAME + "().eval();"))
        );
    }

    private String[] writeContinuationSource(CompiledCaseCont compiledCaseCont) {
        return list(
                d("public CodeLabel[] " + compiledCaseCont.returnVectorName + " = " +
                        Arrays.stream(compiledCaseCont.returnVector)
                                .map(c -> "this::" + c)
                                .collect(Collectors.joining(", ", "new CodeLabel[]{", "}"))),
                flatten(compiledCaseCont.alternativesSource.entrySet().stream().map(e -> list(
                        m("public CodeLabel " + e.getKey() + "()", compiledCaseCont.alternativesDesc.get(e.getKey())), block(e.getValue())
                ))));
    }

    private String[] declareConstructor(String name) {
        return d("public InfoTable "+ constructorTable(name) + " = new InfoTable(this::cons_" + name + "_entry)", name);
    }

    private String constructorTable(String name) {
        return "cons_" + name + "_info";
    }

    private String[] writeInfoAndCode(InnerDec innerDec) {
        return list(
                d("InfoTable " + innerDec.infoPtr + " = new InfoTable(this::" + innerDec.codeEntry + ")"),
                m("public CodeLabel " + innerDec.codeEntry + "()", innerDec.definition), block(innerDec.body)
        );

    }

    private String[] globalBindsFunctions(Bind bind) {
        return list(
                m("public CodeLabel " + lfEntryCode(bind.lf) + "()", bind.lf.toString()), block(
                        lambdaFormEntryBlock(bind.lf)
                )
        );
    }

    private String[] lambdaFormEntryBlock(LambdaForm lf) {
        List<String> source = new ArrayList<>();

        // fixme use common buildBlock method
        var boundVars = Arrays.stream(lf.boundVars).collect(
                Collectors.toMap(Function.identity(), name -> "LOCAL_" + name));

        // TODO argument satisfaction check
        // TODO stack overflow check
        // TODO heap overflow check
        // pop bound vars
        int nvars = lf.boundVars.length;
        source.addAll(List.of(list(
                indexMap(lf.boundVars).entrySet().stream()
                        .map(e -> e("final Object " + boundVars.get(e.getKey()) + " = " + readA(e.getValue()) + ";"))
                        .flatMap(Arrays::stream).toArray(String[]::new),
                e(popA(nvars), "adjust popped stack all!")
        )));

        Expr expr = lf.expr;

        source.addAll(codeToEvaluateExpression(expr, boundVars, indexMap(lf.freeVars)));

        return source.toArray(String[]::new);
    }

    private static Map<Variable, Integer> indexMap(Variable[] vars) {
        return IntStream.range(0, vars.length).boxed()
                .collect(Collectors.toMap(i -> vars[i], Function.identity()));
    }

    private List<String> codeToEvaluateExpression(Expr expr, Map<Variable, String> boundVars, Map<Variable, Integer> freeVarsIndex) {
        List<String> source = new ArrayList<>();

        if (expr instanceof Literal) {
            source.addAll(List.of(list(
                    e("ReturnInt = " + ((Literal) expr).value + ";", "value to be returned"),
                    doOnShortB(e("return this::MAIN_RETURN_INT;")),
                    e("return (CodeLabel) B[SpB--];"))));
        } else if (expr instanceof Application) {
            source.addAll(List.of(
                    comment("Function application expression")
            ));

            var call = ((Application) expr);

            for (int i = 0; i < call.args.length; i++) {
                if (call.args[i] instanceof Literal) {
                    var lit = (Literal)call.args[i];
                    source.addAll(List.of(
                            e(pushA(String.valueOf(lit.value)), "add arguments")));
                } else {
                    throw new CompileFailed("FIXME not implemented arguments that are not literals!!!");
                }
            }

            String call_f_name = resolve_variable(call.f, boundVars, freeVarsIndex);

            // call to non-built-in function
            source.addAll(List.of(list(
                    e("Node = (Closure) " + call_f_name + ";", "entering the closure"),
                    e("return ENTER(Node);")
            )));

        } else if (expr instanceof Let) {
            source.addAll(List.of(
                    comment("Evaluating LET expression")
            ));
            var let = (Let) expr;
            if (let.isRec) {
                throw new CompileFailed("FIXME not implemented recursive: !!! " + expr.toString());
            }

            // prepare code to be used in evaluation
            var compiledBinds = new HashMap<Variable, InnerDec>();
            for (var bind: let.binds) {
                var inner = new InnerDec(bind.var.name, lambdaFormEntryBlock(bind.lf), bind.lf.toString());
                inners.add(inner);
                compiledBinds.put(bind.var, inner);
            }

            // allocate closures
            for (var bind: let.binds) {
                source.addAll(List.of(list(
                        // TODO send bound variables via closure
                        e("final Closure local_" + bind.var.name + "_closure  = new Closure(" + compiledBinds.get(bind.var).infoPtr + ");"),
                        e("H[++Hp] = local_" + bind.var.name + "_closure;", "put on heap")
                )));
            }

            // add to bound variables environment
            Map<Variable, String> innerBoundVars = new HashMap<>(boundVars);
            for (var bind: let.binds) {
                innerBoundVars.put(bind.var, "local_" + bind.var.name + "_closure");
            }
            source.addAll(codeToEvaluateExpression(let.expr, innerBoundVars, freeVarsIndex));
        } else if (expr instanceof Case) {
            source.addAll(List.of(
                    comment("Evaluating CASE expression")
            ));

            source.addAll(List.of(
                    comment("Saving local environment...")));
            // save free and bound vars
            var localEnvIdxs = new HashMap<Variable, Integer>();
            for(var boundVar: boundVars.keySet()) {
                localEnvIdxs.put(boundVar, localEnvIdxs.size());
                source.addAll(List.of(e(pushA(boundVars.get(boundVar)))));
            }

            for(var freeVar: freeVarsIndex.keySet()) {
                localEnvIdxs.put(freeVar, localEnvIdxs.size());
                source.addAll(List.of(e(pushA(boundVars.get(freeVar)))));
            }

            var _case = (Case)expr;
            var compiledCaseCont = compileCaseCont(_case, localEnvIdxs);
            continuations.add(compiledCaseCont);

            source.addAll(List.of(list(
                    e("B[++SpB] = " + compiledCaseCont.returnVectorName + ";", "Push continuation return vector"),
                    codeToEvaluateExpression(_case.expr, boundVars, freeVarsIndex).toArray(String[]::new)
            )));
        } else if (expr instanceof Cons) {
            var cons = (Cons<Atom>)expr;
            var name = cons.cons;
            var args = Arrays.stream(cons.args).map(
                    atom -> {
                        if (atom instanceof Literal) {
                            return String.valueOf(((Literal) atom).value);
                        } else if (atom instanceof Variable) {
                            return resolve_variable((Variable)atom, boundVars, freeVarsIndex);
                        } else {
                            throw new RuntimeException("unrecognized atom: " + atom);
                        }
                    }
            ).collect(Collectors.toList());

            source.addAll(List.of(list(
                    e("final Closure constructed = new Closure("
                            + constructorTable(name) + ", "
                            + "null, "
                            + args.stream().collect(Collectors.joining(", ", "new Object[]{", "}"))+ ");" ),
                    e("Node = constructed;"),
                    e("return ENTER(Node);")
            )));
        } else {
            throw new CompileFailed("FIXME not implemented: !!! " + expr.toString());
        }

        return source;
    }

    private String[] doOnShortB(String[] doIt) {
        return list(
            e("if (SpB < 0)", "FIXME compare to stack head"), block(doIt)
        );
    }

    private CompiledCaseCont compileCaseCont(Case _case, HashMap<Variable, Integer> localEnvIdxsOnStack) {
        if (_case.alts[0] instanceof AlgAlt) {
            var default_ = select(_case.alts, DefaultAlt.class);
            var algAlts = select(_case.alts, AlgAlt.class);
            ensure(default_.size() + algAlts.size() == _case.alts.length, "Unrecognized alternative type in " + _case.toString());

            var classEnums = algAlts.stream().map(a -> a.cons.cons).map(constructorClass::get).distinct().collect(Collectors.toList());
            if (classEnums.size() == 0 || classEnums.stream().anyMatch(Objects::isNull)) {
                throw new CompileFailed("FIXME not implemented undefined algebraic class?! " + _case.toString());
            }
            ensure(classEnums.size() == 1, "Can't have more than one class in an algebraic alternative: " + classEnums.toString());

            var classEnum = classEnums.get(0);
            var allConstructors = classConstructors.get(classEnum);
            ensure(algAlts.stream().allMatch(a -> allConstructors.contains(a.cons.cons)), "Some constructor is not available!");

            var alternatives = allConstructors.stream().collect(Collectors.toMap(
                    Function.identity(),
                    c -> algAlts.stream().filter(a -> a.cons.cons.equals(c)).findFirst()));
            ensure (alternatives.values().stream().allMatch(Optional::isPresent), "Some alternatives are undefined!");

            var alternativesCode = alternatives.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> compileAlternative(e.getKey(), e.getValue().get(), localEnvIdxsOnStack)));

            return new CompiledCaseCont(classEnum, allConstructors, alternativesCode, alternatives);
        } else if (_case.alts[1] instanceof PrimAlt) {
            throw new CompileFailed("FIXME not implemented primitive alternatives: !!! " + _case.toString());
        } else {
            throw new CompileFailed("FIXME not implemented alternatives: !!! " + _case.toString());
        }
    }

    private String[] compileAlternative(String classConstructor, AlgAlt alternative, Map<Variable, Integer> localEnvIdxsOnStack) {

        var source = new ArrayList<String>();

        source.addAll(List.of(list(
                comment("convention:"),
                comment(" - Node contains the constructed closure"),
                comment(" - the other arguments are on the stack!"))));

        var newNames = new HashMap<Variable, String>();

        // pop from stack
        for(var entry: localEnvIdxsOnStack.entrySet()) {
            newNames.put(entry.getKey(), "local$" + entry.getKey().name);
            source.addAll(List.of(
                    e("final Object " + newNames.get(entry.getKey()) + " = A[SpA - " + entry.getValue() + "];", "read " + entry.getKey().name)));
        }
        source.addAll(List.of(
                e(popA(localEnvIdxsOnStack.size()), "pop all passed")));

        // read arguments from Node
        for (int i = 0; i < alternative.cons.args.length; i++) {
            var arg = alternative.cons.args[i];
            newNames.put(arg, "passed$" + arg.name);

            source.addAll(List.of(list(
                    e("final Object " + newNames.get(arg) + " = Node.nonPointerWords[" + i + "];", "read " + arg.name)
            )));
        }

        source.addAll(codeToEvaluateExpression(alternative.expr, newNames, Collections.emptyMap()));

        return source.toArray(String[]::new);
    }

    private <T, R> List<R> select(T[] vals, Class<R> clazz) {
        return Arrays.stream(vals).filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
    }

    private String resolve_variable(Variable f, Map<Variable, String> boundVars, Map<Variable, Integer> freeVarsIndex) {
        if (freeVarsIndex.containsKey(f)) {
            // todo use free vars as well!
            throw new CompileFailed("FIXME not implemented passing via free vars!!!" + f.toString());
        } else if (boundVars.containsKey(f)) {
            return boundVars.get(f);
        } else if (globalNames.containsKey(f.name)) {
            return globalNames.get(f.name);
        } else {
            ensure(false, "Can't find variable " + f + " in any environment!");
            throw new RuntimeException();
        }
    }

    private String pushA(String value) {
        return "A[--SpA]" + " = " + value + ";";
    }

    private String popA(int nvars) {
        return "SpA = SpA + " + nvars + ";";
    }

    private String readA(int offset) {
        return "A[SpA + " + offset + "]";
    }

    private String[] dumpA() {
        return list(e(dump("SpA = \" + SpA + \"")),
                e(dump("A = ...\" + Arrays.toString(IntStream.range(SpA-10, A.length).mapToObj(i -> A[i]).toArray(Object[]::new)) + \"")));
    }

    private String lfEntryCode(LambdaForm lf) {
        return lfNames.computeIfAbsent(lf, l -> "LF_" + (lfIter++) + "_entry");
    }

    private String[] flatten(Stream<String[]> vals) {
        return  vals.flatMap(Arrays::stream).toArray(String[]::new);
    }

    private Stream<Bind> globalNonMainBinds() {
        return graph.values().stream()
                .filter(b -> !b.var.name.equals(ENTRY_POINT));
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
        return new String[]{s + inlineComment(comment)};
    }

    private static String inlineComment(String... comment) {
        return comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", "/* ", " */"));
    }

    static private String[] c(String classDef, String... comments) {
        return new String[]{"" + inlineComment(comments), classDef + " "};
    }

    static String[] block(String[]... elems) {
        return Stream.of(
                Stream.of("{"),
                Arrays.stream(elems).flatMap(Arrays::stream).map(s -> OFFSET + s),
                Stream.of("}")).flatMap(Function.identity()).toArray(String[]::new);
    }

    static private String[] d(String v, String... comment) {
        return new String[]{v + ";" + (comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", " // ", "")))};
    }


    static String[] list(String[]... strs) {
        return Arrays.stream(strs).flatMap(Arrays::stream).toArray(String[]::new);
    }

    public String mainClassName() {
        return MAIN_CLASS_NAME;
    }

    private static String[] comment(String text) {
        return new String[]{"/* ", text, " */"};
    }

    public String compile() {
        if (src == null) {
            var lines = list(
                    e("import java.util.*;"),
                    e("import java.util.stream.IntStream;"),

                    c("public class " + MAIN_CLASS_NAME, description), mainClassBody()
            );
            src = Arrays.stream(lines).collect(Collectors.joining("\n", "", ""));
        }
        return src;
    }

    public String withLNs() {
        var lines = compile().split("\n");
        return IntStream.range(0, lines.length).mapToObj(i -> String.format("%03d\t%s", i+1, lines[i])).collect(Collectors.joining("\n", "", ""));
    }

    private class InnerDec {
        private final String[] body;
        public final String definition;

        public final String infoPtr;
        public final String codeEntry;

        public InnerDec(String name, String[] body, String definition) {
            this.body = body;
            this.definition = definition;

            var idx = nextInner++;
            this.infoPtr = "inner$" + idx + name + "_info";
            this.codeEntry = "inner$" + idx + name + "_entry";
        }
    }

    private class CompiledCaseCont {
        public final String returnVectorName;
        public final String[] returnVector;
        private final Map<String, String[]> alternativesSource;
        private final Map<String, String> alternativesDesc;

        private CompiledCaseCont(String classEnum, List<String> allConstructors, Map<String, String[]> alternativesSource, Map<String, Optional<AlgAlt>> alternativesOriginalSource) {
            var prefix = classEnum + "$" + (nextCont ++) + "$" ;
            Function<String, String> continuationCodeFun = c -> prefix + c + "_cont";
            returnVectorName = prefix + "RetVec";
            this.returnVector = allConstructors.stream().map(continuationCodeFun).toArray(String[]::new);
            this.alternativesSource = alternativesSource.entrySet().stream().collect(Collectors.toMap(
                    e -> continuationCodeFun.apply(e.getKey()),
                    Map.Entry::getValue));
            this.alternativesDesc = alternativesOriginalSource.entrySet().stream().collect(Collectors.toMap(
                    e -> continuationCodeFun.apply(e.getKey()),
                    e -> e.getValue().get().toString()
            ));
        }
    }
}

class CompilationFailed extends RuntimeException {
    public CompilationFailed(String s) {
        super(s);
    }
}
