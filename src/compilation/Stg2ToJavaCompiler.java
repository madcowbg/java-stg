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


    enum VariableType {Polymorphic, Primitive;}

    enum ResolutionType {Bound, Global;}

    private static final String ENTRY_POINT = "main";
    private static final String MAIN_CLASS_NAME = "MainAppClass";
    private static final String OFFSET = "    ";

    private boolean debuggingEnabled = true;

    private final String description;

    private final Map<String, Bind> graph;
    private final Map<String, CompiledBind> globalBinds;

    private final HashMap<Variable, String> lfNames = new HashMap<>();
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
        this.globalBinds = this.graph.values().stream().map(CompiledBind::new).collect(Collectors.toMap(CompiledBind::varName, Function.identity()));

        Bind main = this.graph.get("main");
        ensure(main.lf.boundVars.length == 0, "main can't have bound variables");
    }

    private String globalClosureName(Variable var) {
        return "$" + var.name + "$_closure";
    }

    private String globalInfoName(Variable var) {
        return "$" + var.name + "$_info";
    }

    private class CompiledBind {
        private final Bind bind;

        private final String[] declareInfoTable;
        private final String[] declareClosure;

        public CompiledBind(Bind bind) {
            this.bind = bind;

            declareInfoTable =
                    d("public InfoTable " + globalInfoName(bind.var) + " = new InfoTable(this::" + lfEntryCode(bind.var) + ")");
            declareClosure =
                    d("public Closure " + globalClosureName(bind.var) + " = new Closure(" + globalInfoName(bind.var) + ")");
        }

        private final String[] declareEntryFunction() {
            var compiledLF = new CompiledLambdaForm(bind.lf);

            // TODO figure out the type!
            return list(
                    m("public CodeLabel " + lfEntryCode(bind.var) + "()", bind.lf.toString()), block(
                            debug(e(dump("ENTER: " + sanitize(bind.lf.toString())))),
                            compiledLF.body,
                            compiledLF.jump
                    ));
        }

        public String[] toCode() {
            return list(
                    declareInfoTable,
                    declareClosure,
                    declareEntryFunction()
            );
        }

        public String varName() {
            return bind.var.name;
        }
    }

    private class CompiledLambdaForm {
        private final String[] body;
        private final String[] jump;

        CompiledLambdaForm(LambdaForm lf) {
            var localEnv = environmentFor(lf);

            // pop bound vars
            int nvars = lf.boundVars.length;
            var popBoundVarsCode = list(
                    indexMap(lf.boundVars).entrySet().stream()
                            .map(e -> e("final Object " + localEnv.boundVarNames.get(e.getKey()) + " = " + readA(e.getValue()) + ";"))
                            .flatMap(Arrays::stream).toArray(String[]::new),
                    e(popA(nvars), "adjust popped stack all!"));

            var compiledExpr = new CompiledExpression(lf.expr, localEnv);;
            this.body = list(
                    // TODO argument satisfaction check
                    // TODO stack overflow check
                    // TODO heap overflow check

                    // pop bound vars
                    popBoundVarsCode,

                    // evaluate the expression
                    compiledExpr.body
            );
            this.jump = compiledExpr.jump;
        }
    }

    private String[] mainClassBody() {
        return block(
                c("class Closure"), block(
                        d("public final InfoTable infoPointer"),
                        d("public Closure[] pointerWords"),      // FIXME typize?
                        d("public Object[] nonPointerWords"),  // FIXME typize?
                        m("Closure(InfoTable infoPointer)"), block(e("this.infoPointer = infoPointer;")),
                        m("Closure(InfoTable infoPointer, Closure[] pointerWords, Object[] nonPointerWords)"), block(
                                e("this.infoPointer = infoPointer;"),
                                e("this.pointerWords = pointerWords;"),
                                e("this.nonPointerWords = nonPointerWords;"))),

                c("class InfoTable"), block(
                        d("public final CodeLabel standardEntryCode"),
                        d("public CodeLabel evacuationCode"),
                        d("public CodeLabel scavengeCode"),
                        m("InfoTable(CodeLabel standardEntryCode)"), block(e("this.standardEntryCode = standardEntryCode;"))),

                comment("Global infos, closures and entry functions"),
                flatten(globalBinds.values().stream().map(CompiledBind::toCode)),

                comment("Constructors"),
                flatten(constructorClass.keySet().stream().map(this::declareConstructor)), // declare all constructors

                m("public CodeLabel cons_MkInt_entry()"), block(
                        debug(e(dump("entering MkInt"))),
                        doOnShortB(list(
                                debug(e(dump("returning MkInt from eval"))),
                                e("result = new Object[]{\"MkInt\", Node.nonPointerWords[0]};", "argument in first non-pointer"),
                                e("return null;")
                        )),
                        e("final CodeLabel[] conts = (CodeLabel[]) B[SpB--];", "pop continuation vector"),
                        e("return conts[0];", "single constructor, so we know it is the first one!")
                ),

                c("interface CodeLabel", "Represents pieces of code"), block(
                        d("CodeLabel jumpTo()")
                ),

                m("public CodeLabel MAIN_RETURN_INT()"), block(
                        debug(e(dump("MAIN RETURN integer: \" + ReturnInt +\""))),
                        e("result = new Integer(ReturnInt);"),
                        e("return null;")
                ),

                m("public CodeLabel EVAL_MAIN()"), block(
                        debug(e(dump("EVAL_MAIN"))),
                        e("Node = $" + ENTRY_POINT + "$_closure;"),
                        e("return ENTER(Node);")
                ),

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
                flatten(inners.stream().map(InnerDec::writeInfoAndCode)),

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
        return d("public InfoTable " + constructorTable(name) + " = new InfoTable(this::cons_" + name + "_entry)", name);
    }

    private String constructorTable(String name) {
        return "cons_" + name + "_info";
    }

    private static String sanitize(String toString) {
        return toString.replace("\\n", "\\\\n");
    }

    private static Map<Variable, Integer> indexMap(Variable[] vars) {
        return IntStream.range(0, vars.length).boxed()
                .collect(Collectors.toMap(i -> vars[i], Function.identity()));
    }

    interface CompiledArgument {
        String[] pushToStack();
    }

    class CompiledLiteral implements CompiledArgument {
        private final int value;

        public CompiledLiteral(int value) {
            this.value = value;
        }

        @Override
        public String[] pushToStack() {
            return e(pushA(String.valueOf(value)), "push literal argument"); // FIXME use stack B
        }
    }

    private class CompiledExpression {
        private final String[] body;
        private final String[] jump;

        CompiledExpression(Expr expr, LocalEnvironment env) {
            if (expr instanceof Literal) {
                body = list(
                        e("ReturnInt = " + ((Literal) expr).value + ";", "value to be returned"),
                        doOnShortB(e("return this::MAIN_RETURN_INT;")));
                jump =
                        e("return (CodeLabel) B[SpB--];");
            } else if (expr instanceof Application) {
                var call = ((Application) expr);
                var args = Arrays.stream(call.args).map(this::compileArgument);

                var resolution = env.resolve(call.f);
                if (resolution.type == ResolutionType.Global) {
                    // we know what the function is, provide argument types as requested
                    // TODO implement
                }
                String call_f_name = resolution.resolvedName;

                body = list(
                        comment("Function application expression"),
                        // push arguments to appropriate stacks
                        flatten(args.map(CompiledArgument::pushToStack))
                );

                jump = list(
                        // call to non-built-in function
                        e("Node = (Closure) " + call_f_name + ";", "entering the closure"),
                        e("return ENTER(Node);")
                );
            } else if (expr instanceof Let) {
                List<String> source = new ArrayList<>();
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
                    var inner = new InnerDec(bind.var.name, new CompiledLambdaForm(bind.lf), bind.lf.toString());
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
                LocalEnvironment inner = env.rebind(let.binds);

                var letExpr = new CompiledExpression(let.expr, inner);
                source.addAll(List.of(letExpr.body));
                body = source.toArray(String[]::new);

                jump = letExpr.jump;
            } else if (expr instanceof Case) {
                List<String> source = new ArrayList<>();
                source.addAll(List.of(
                        comment("Evaluating CASE expression")
                ));

                source.addAll(List.of(
                        comment("Saving local environment...")));
                // save free and bound vars

                var stackSave = env.saveToStack();
                source.addAll(stackSave.source);

                var _case = (Case) expr;
                var compiledCaseCont = compileCaseCont(_case, stackSave.localEnvIdxs);
                continuations.add(compiledCaseCont);

                var caseExpr = new CompiledExpression(_case.expr, env);
                source.addAll(List.of(list(
                        e("B[++SpB] = " + compiledCaseCont.returnVectorName + ";", "Push continuation return vector"),
                        caseExpr.body
                )));

                body = source.toArray(String[]::new);
                jump = caseExpr.jump;
            } else if (expr instanceof Cons) {
                var cons = (Cons<Atom>) expr;
                var name = cons.cons;
                var args = Arrays.stream(cons.args).map(
                        atom -> {
                            if (atom instanceof Literal) {
                                return String.valueOf(((Literal) atom).value);
                            } else if (atom instanceof Variable) {
                                return env.resolve((Variable) atom).resolvedName;
                            } else {
                                throw new RuntimeException("unrecognized atom: " + atom);
                            }
                        }
                ).collect(Collectors.toList());

                List<String> source = new ArrayList<>();
                source.addAll(List.of(list(
                        e("final Closure constructed = new Closure("
                                + constructorTable(name) + ", "
                                + "null, "
                                + args.stream().collect(Collectors.joining(", ", "new Object[]{", "}")) + ");"))));
                body = source.toArray(String[]::new);

                jump = list(
                        e("Node = constructed;"),
                        e("return ENTER(Node);")
                );
            } else {
                throw new CompileFailed("FIXME not implemented: !!! " + expr.toString());
            }
        }

        private CompiledArgument compileArgument(Atom a) {
            if (a instanceof Literal) {
                return new CompiledLiteral(((Literal) a).value);
            } else {
                // FIXME implement
                throw new CompileFailed("FIXME not implemented arguments that are not literals!!!");
            }
        }
    }

    private static String[] doOnShortB(String[] doIt) {
        return list(
                e("if (SpB < 0)", "FIXME compare to stack head"), block(doIt)
        );
    }

    private CompiledCaseCont compileCaseCont(Case _case, LocalEnvironment.SavedOnStack localEnvIdxsOnStack) {
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
            ensure(alternatives.values().stream().allMatch(Optional::isPresent), "Some alternatives are undefined!");

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

    private String[] compileAlternative(String classConstructor, AlgAlt alternative, LocalEnvironment.SavedOnStack localEnvIdxsOnStack) {
        var popStackToLocal = localEnvIdxsOnStack.stackPopAndReadNode(alternative.cons.args);
        var altExp = new CompiledExpression(alternative.expr, popStackToLocal.environment);

        return list(
                comment(
                        "convention:",
                        " - Node contains the constructed closure",
                        " - the other arguments are on the stack!"),

                // pop from stack
                popStackToLocal.source.toArray(String[]::new),
                altExp.body,
                altExp.jump);
    }

    private <T, R> List<R> select(T[] vals, Class<R> clazz) {
        return Arrays.stream(vals).filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
    }

    private String pushB(String value) {
        return "B[++SpB] = " + value;
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

    private String lfEntryCode(Variable v) {
        return lfNames.computeIfAbsent(v, var -> "$" + var + "$_entry");
    }

    private String[] flatten(Stream<String[]> vals) {
        return vals.flatMap(Arrays::stream).toArray(String[]::new);
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
        return Stream.of(
                Arrays.stream(comment(comments)),
                Stream.of(s)).flatMap(Function.identity()).toArray(String[]::new);
    }

    static private String[] e(String s, String... comment) {
        return new String[]{s + inlineComment(comment)};
    }

    private static String inlineComment(String... comment) {
        return comment.length == 0 ? "" : Arrays.stream(comment).collect(Collectors.joining(" ", " /* ", " */"));
    }

    static private String[] c(String classDef, String... comments) {
        return Stream.concat(
                Arrays.stream(comment(comments)),
                Stream.of(classDef + " ")).toArray(String[]::new);
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

    private static String[] comment(String... text) {
        return text.length == 0 ? new String[]{} : Stream.concat(Stream.concat(
                Stream.of("/* "),
                Arrays.stream(text).map(s -> "   " + s)),
                Stream.of(" */")).toArray(String[]::new);
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
        return IntStream.range(0, lines.length).mapToObj(i -> String.format("%03d\t%s", i + 1, lines[i])).collect(Collectors.joining("\n", "", ""));
    }

    private class InnerDec {
        private final CompiledLambdaForm compiledLF;

        public final String comments;

        public final String infoPtr;
        public final String codeEntry;

        public InnerDec(String name, CompiledLambdaForm compiledLF, String comments) {
            this.comments = comments;

            this.compiledLF = compiledLF;

            var idx = nextInner++;
            this.infoPtr = "inner$" + idx + name + "_info";
            this.codeEntry = "inner$" + idx + name + "_entry";
        }

        private String[] writeInfoAndCode() {
            return list(
                    d("InfoTable " + infoPtr + " = new InfoTable(this::" + codeEntry + ")"),

                    m("public CodeLabel " + codeEntry + "()", comments), block(
                            debug(e(dump("ENTER: " + sanitize(comments)))),
                            compiledLF.body,
                            compiledLF.jump
                    )
            );
        }
    }

    private class CompiledCaseCont {
        public final String returnVectorName;
        public final String[] returnVector;
        private final Map<String, String[]> alternativesSource;
        private final Map<String, String> alternativesDesc;

        private CompiledCaseCont(String classEnum, List<String> allConstructors, Map<String, String[]> alternativesSource, Map<String, Optional<AlgAlt>> alternativesOriginalSource) {
            var prefix = classEnum + "$" + (nextCont++) + "$";
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

    public LocalEnvironment environmentFor(LambdaForm lf) {
        return new LocalEnvironment(lf.boundVars, lf.freeVars);
    }

    public class LocalEnvironment {
        private final Map<Variable, String> boundVarNames;

        private final Map<Variable, Integer> freeVarsIndexMap;

        public LocalEnvironment(Variable[] boundVars, Variable[] freeVars) {
            this.freeVarsIndexMap = indexMap(freeVars);

            this.boundVarNames = Arrays.stream(boundVars).collect(
                    Collectors.toMap(Function.identity(), name -> "LOCAL_" + name));
        }

        public LocalEnvironment(Map<Variable, String> innerBoundVars, Map<Variable, Integer> freeVarsIndexMap) {
            this.boundVarNames = innerBoundVars;
            this.freeVarsIndexMap = freeVarsIndexMap;
        }

        public LocalEnvironment rebind(Bind[] newBinds) {
            Map<Variable, String> innerBoundVars = new HashMap<>(boundVarNames);
            for (var bind : newBinds) {
                innerBoundVars.put(bind.var, "local_" + bind.var.name + "_closure");
            }
            return new LocalEnvironment(innerBoundVars, freeVarsIndexMap);
        }

        public Resolution resolve(Variable f) {
            if (freeVarsIndexMap.containsKey(f)) {
                // todo use free vars as well!
                throw new CompileFailed("FIXME not implemented passing via free vars!!!" + f.toString());
            } else if (boundVarNames.containsKey(f)) {
                return new Resolution(boundVarNames.get(f), ResolutionType.Bound, f);
            } else {
                ensure(globalBinds.containsKey(f.name), "Can't find variable " + f + " in any environment!");
                return new Resolution(globalClosureName(f), ResolutionType.Global, f);
            }
        }

        public StackSave saveToStack() {
            var source = new ArrayList<String>();

            // TODO optimize - remove those vars that won't be needed for evaluation...

            var localEnvIdxs = new HashMap<Variable, Integer>();
            for (var boundVar : boundVarNames.keySet()) {
                localEnvIdxs.put(boundVar, localEnvIdxs.size());
                source.addAll(List.of(e(pushA(boundVarNames.get(boundVar)))));
            }

            for (var freeVar : freeVarsIndexMap.keySet()) {
                ensure(false, "free var pushing to stack not implemented!!!");

//                localEnvIdxs.put(freeVar, localEnvIdxs.size());
//                source.addAll(List.of(e(pushA(boundVars.get(freeVar)))));
            }

            return new StackSave(source, new SavedOnStack(localEnvIdxs));
        }

        private class StackSave {
            public final List<String> source;
            public final SavedOnStack localEnvIdxs;

            public StackSave(ArrayList<String> source, SavedOnStack localEnvIdxs) {
                this.source = source;
                this.localEnvIdxs = localEnvIdxs;
            }

        }

        private class SavedOnStack {
            public final Map<Variable, Integer> localEnvIdxs;

            public SavedOnStack(HashMap<Variable, Integer> localEnvIdxs) {
                this.localEnvIdxs = localEnvIdxs;
            }

            public StackPop stackPopAndReadNode(Variable[] args) {
                var newNames = new HashMap<Variable, String>();
                var source = new ArrayList<String>();

                for (var entry : localEnvIdxs.entrySet()) {
                    newNames.put(entry.getKey(), "local$" + entry.getKey().name);
                    source.addAll(List.of(
                            e("final Object " + newNames.get(entry.getKey()) + " = A[SpA - " + entry.getValue() + "];", "read " + entry.getKey().name)));
                }
                source.addAll(List.of(
                        e(popA(localEnvIdxs.size()), "pop all passed")));


                // read arguments from Node
                for (int i = 0; i < args.length; i++) {
                    var arg = args[i];
                    newNames.put(arg, "passed$" + arg.name);

                    source.addAll(List.of(list(
                            e("final Object " + newNames.get(arg) + " = Node.nonPointerWords[" + i + "];", "read " + arg.name)
                    )));
                }

                return new StackPop(source, newNames);
            }

        }

        private class StackPop {
            private final ArrayList<String> source;
            private final LocalEnvironment environment;

            public StackPop(ArrayList<String> source, HashMap<Variable, String> newNames) {
                this.source = source;
                this.environment = new LocalEnvironment(newNames, Collections.emptyMap());
            }
        }


        private class Resolution {
            private final String resolvedName;
            private final ResolutionType type;
            private final Variable var;

            public Resolution(String resolvedName, ResolutionType type, Variable var) {
                this.resolvedName = resolvedName;
                this.type = type;
                this.var = var;
            }
        }
    }

}

class CompilationFailed extends RuntimeException {
    public CompilationFailed(String s) {
        super(s);
    }
}
