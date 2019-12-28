package compilation;

import jas.Var;
import tea.core.CompileFailed;
import tea.stg2.parser.Bind;
import tea.stg2.parser.LambdaForm;
import tea.stg2.parser.alt.AlgAlt;
import tea.stg2.parser.alt.Alt;
import tea.stg2.parser.alt.DefaultAlt;
import tea.stg2.parser.alt.PrimAlt;
import tea.stg2.parser.expr.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Stg2ToJavaCompiler {


    enum VariableType {Polymorphic, Primitive, Unrestricted;}

    enum ResolutionType {Bound, Global, Free}

    enum Stack {
        A("A", "SpA", "-"),
        B("B", "SpB", "+");

        public final String name;
        public final String ptr;
        public final String dir;

        Stack(String name, String ptr, String dir) {
            this.name = name;
            this.ptr = ptr;
            this.dir = dir;
        }
    }

    ;

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

    private Map<String, List<VariableType>> constructorArguments = Map.of("MkInt", List.of(VariableType.Primitive)); // TODO these could be declared

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
                    d("public Closure " + globalClosureName(bind.var) + " = new Closure(" + globalInfoName(bind.var) + ", new Object[0], new Object[0])"); // no free vars!
        }

        private final String[] declareEntryFunction() {
            var compiledLF = new CompiledLambdaForm(bind.lf, bind.var.name);

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

    private class PassingConvention {

        private final Variable[] pointerVars;
        private final Variable[] nonPointerVars;

        PassingConvention(Variable[] vars, Predicate<Variable> isPrimitive) {
            pointerVars = Arrays.stream(vars).filter(Predicate.not(isPrimitive)).toArray(Variable[]::new);
            nonPointerVars = Arrays.stream(vars).filter(isPrimitive).toArray(Variable[]::new);
        }

        public boolean isPrimitive(Variable f) {
            return List.of(nonPointerVars).contains(f);
        }

        @Deprecated
        public int size() {
            return pointerVars.length + nonPointerVars.length;
        }
    }

    private class PassedArguments {
        private final PassingConvention convention;
        private final Map<Variable, Address> addresses;

        PassedArguments(PassingConvention convention, Function<Integer, Address> onPointer, Function<Integer, Address> onNonPointer) {
            this.convention = convention;
            this.addresses = Stream.of(
                    indexMap(convention.pointerVars).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> onPointer.apply(e.getValue()))).entrySet(),
                    indexMap(convention.nonPointerVars).entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> onNonPointer.apply(e.getValue()))).entrySet())
                    .flatMap(Set::stream)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        public boolean has(Variable f) {
            return addresses.containsKey(f);
        }

        public Address address(Variable f) {
            assert addresses.containsKey(f);
            return addresses.get(f);
        }

        public boolean isEmpty() {
            return addresses.isEmpty();
        }

        public Set<Variable> variables() {
            return addresses.keySet();
        }

        public boolean isPrimitive(Variable f) {
            return convention.isPrimitive(f);
        }
    }

    private class CompiledLambdaForm {
        private final String[] body;
        private final String[] jump;

        private final PassingConvention boundVars;
        private final PassingConvention freeVars;

        CompiledLambdaForm(LambdaForm lf, String lambdaDebugName) {
            Predicate<Variable> isPrimitiveInExpression = var -> typeOf(var, lf.expr).equals(VariableType.Primitive);
            boundVars = new PassingConvention(lf.boundVars, isPrimitiveInExpression);
            freeVars = new PassingConvention(lf.freeVars, isPrimitiveInExpression);

            var localEnv = new LocalEnvironment(boundVars, freeVars);
            var compiledExpr = new CompiledExpression(lf.expr, localEnv);

            this.body = list(
                    // TODO make partial application work
                    e("if (SpA + " + boundVars.pointerVars.length + " > A.length)", "not enough pointer parameters..."), block(
                            e("throw new RuntimeException(\"" + lambdaDebugName + " needs " + boundVars.pointerVars.length + " pointer params, got \" + (A.length - SpA) + \"!\");")
                    ),
                    e("if (SpB - " + boundVars.nonPointerVars.length + " < -1)", "not enough non-pointer parameters..."), block(
                            e("throw new RuntimeException(\"" + lambdaDebugName + " needs " + boundVars.nonPointerVars.length + " non-pointer params, got \" + (SpB) + \"!\");")
                    ),

                    // TODO stack overflow check
                    // TODO heap overflow check

                    // pop bound vars
                    e(popA(boundVars.pointerVars.length), "pop all pointer arguments"),
                    e(popB(boundVars.nonPointerVars.length), "pop all non-pointer arguments!"),

                    // evaluate the expression
                    compiledExpr.body
            );
            this.jump = compiledExpr.jump;
        }
    }

    private VariableType typeOf(Variable var, Expr expr) {
        if (expr instanceof Literal) {
            return VariableType.Unrestricted;
        } else if (expr instanceof Application) {
            var app = (Application) expr;
            if (var.equals(app.f)) {
                return VariableType.Polymorphic;
            } else if (List.of(app.args).contains(var)) {
                // TODO check type of function maybe? globals can be resolved...
                return VariableType.Unrestricted;
            } else {
                return VariableType.Unrestricted;
            }
        } else if (expr instanceof Case) {
            var _case = (Case) expr;
            var inExpr = typeOf(var, _case.expr);
            if (inExpr != VariableType.Unrestricted) {
                return inExpr;
            }
            var altRest = Arrays.stream(_case.alts).map(alt -> restrictionByAlternative(var, alt))
                    .filter(Predicate.not(VariableType.Unrestricted::equals))
                    .distinct().collect(Collectors.toList());

            if (altRest.size() == 0) {
                return VariableType.Unrestricted;
            }

            ensure(altRest.size() == 1, "" + var.name + " is used both primitive and polymorphic in " + expr);

            return altRest.get(0);
        } else if (expr instanceof Cons) {
            var cons = (Cons<Atom>) expr;
            var expectedArgTypes = constructorArguments.get(cons.cons);
            var usedAtIdx = List.of(cons.args).indexOf(var);
            if (usedAtIdx == -1) {
                return VariableType.Unrestricted;
            }
            return expectedArgTypes.get(usedAtIdx);
        } else if (expr instanceof Let) {
            var let = (Let) expr;
            if (let.isRec) {
                throw new RuntimeException("FIXME implement recursive let: " + expr);
            }

            var declRestr = Arrays.stream(let.binds)
                    .map(b -> Arrays.asList(b.lf.freeVars).contains(var) ? typeOf(var, b.lf.expr) : VariableType.Unrestricted)
                    .filter(Predicate.not(VariableType.Unrestricted::equals))
                    .distinct().collect(Collectors.toList());

            if (declRestr.size() == 1) {
                return declRestr.get(0);
            }
            ensure(declRestr.size() == 0, var.name + " is used both primitive and polymorphic in " + expr);

            if (Arrays.stream(let.binds).map(b -> b.var).noneMatch(var::equals)) {
                return typeOf(var, let.expr);
            } else {
                // ... is hidden by declaration
                return VariableType.Unrestricted;
            }
        } else {
            throw new RuntimeException("FIXME implement type of: " + expr);
        }
    }

    private VariableType restrictionByAlternative(Variable var, Alt alt) {
        if (alt instanceof AlgAlt) {
            var aalt = (AlgAlt) alt;
            if (List.of(aalt.cons.args).contains(var)) {
                // masked by alternative
                return VariableType.Unrestricted;
            }
            return typeOf(var, aalt.expr);
        } else {
            throw new RuntimeException("FIXME implement alternative type of: " + alt);
        }
    }

    private String[] mainClassBody() {
        return block(
                c("class Closure"), block(
                        d("public final InfoTable infoPointer"),
                        d("public Object[] pointerWords"),      // FIXME typize to Closure
                        d("public Object[] nonPointerWords"),  // FIXME typize?
                        m("Closure(InfoTable infoPointer, Object[] pointerWords, Object[] nonPointerWords)"), block(
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
                d("private Closure[] A = new Closure[1000]; "), // FIXME implement dynamic allocation and GC
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

                debug(list(
                        m("public static void main(String[] args)", "for debugging purposes only"), block(
                                e("new " + MAIN_CLASS_NAME + "().eval();"))))
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

        boolean isPrimitive();

        String address();
    }

    class CompiledLiteral implements CompiledArgument {
        private final int value;

        public CompiledLiteral(int value) {
            this.value = value;
        }

        @Override
        public String[] pushToStack() {
            return e(pushB(String.valueOf(value)), "push literal argument");
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public String address() {
            return String.valueOf(value);
        }
    }

    class CompiledAddress implements CompiledArgument {

        private final boolean isPrimitive;
        private final String addr;

        public CompiledAddress(LocalEnvironment.Resolution resolve) {
            this.isPrimitive = resolve.isPrimitive;
            this.addr = resolve.resolvedName;
        }

        @Override
        public String[] pushToStack() {
            return e((isPrimitive ? pushB(addr) : pushA(addr)));
        }

        @Override
        public boolean isPrimitive() {
            return isPrimitive;
        }

        @Override
        public String address() {
            return addr;
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
                var args = Arrays.stream(call.args).map(compileArgument(env));

                var resolution = env.resolve(call.f);
                if (resolution.type == ResolutionType.Global) {
                    // we know what the function is, provide argument types as requested
                    // TODO implement type checks
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
                for (var bind : let.binds) {
                    var inner = new InnerDec(bind.var.name, new CompiledLambdaForm(bind.lf, bind.var.name), bind.lf.toString());
                    inners.add(inner);
                    compiledBinds.put(bind.var, inner);
                }

                // add to bound variables environment
                LocalEnvironment inner = env.rebind(let.binds);

                // allocate closures
                for (var bind : let.binds) {
                    var compiledBind = compiledBinds.get(bind.var);

                    var argumentValues = Arrays.stream(bind.lf.freeVars).map(inner::resolve).collect(Collectors.toList());

                    var callConvention = compiledBind.compiledLF.boundVars;
                    validateConvention(callConvention, argumentValues);

                    var pointerArgs = argumentValues.stream().filter(r -> !r.isPrimitive).map(r -> r.resolvedName).collect(Collectors.toSet());
                    var primitiveArgs = argumentValues.stream().filter(r -> r.isPrimitive).map(r -> r.resolvedName).collect(Collectors.toSet());

                    var passedPointerVars = "new Object[] {" + String.join(", ", pointerArgs) + "}";
                    var passedPrimitiveVars = "new Object[] {" + String.join(", ", primitiveArgs) + "}";

                    source.addAll(List.of(list(
                            // TODO send free variables via closure
                            e("final Closure local_" + bind.var.name + "_closure  = new Closure("
                                    + compiledBind.infoPtr + ","
                                    + passedPointerVars + ","
                                    + passedPrimitiveVars + " );"),
                            e("H[++Hp] = local_" + bind.var.name + "_closure;", "put on heap")
                    )));
                }

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
                // TODO implement type checks

                // save free and bound vars

                var stackSave = env.saveToStack();
                source.addAll(stackSave.source);

                var _case = (Case) expr;
                var compiledCaseCont = compileCaseCont(_case, stackSave.stackConvention);
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

                var resolvedArgs = Arrays.stream(cons.args).map(compileArgument(env)).collect(Collectors.toList());

                // TODO implement passed arguments type checks

                var pointerArgs = resolvedArgs.stream().filter(Predicate.not(CompiledArgument::isPrimitive)).map(CompiledArgument::address).collect(Collectors.toList());
                var primitiveArgs = resolvedArgs.stream().filter(CompiledArgument::isPrimitive).map(CompiledArgument::address).collect(Collectors.toList());

                List<String> source = new ArrayList<>();
                source.addAll(List.of(list(
                        e("final Closure constructed = new Closure("
                                + constructorTable(name) + ", "
                                + "new Object[]{" + String.join(", ", pointerArgs) +"}, "
                                + "new Object[]{" + String.join(", ", primitiveArgs) +"});"))));
                body = source.toArray(String[]::new);

                jump = list(
                        e("Node = constructed;"),
                        e("return ENTER(Node);")
                );
            } else {
                throw new CompileFailed("FIXME not implemented: !!! " + expr.toString());
            }
        }

        private Function<Atom, CompiledArgument> compileArgument(LocalEnvironment env) {
            return (Atom a) -> {
                if (a instanceof Literal) {
                    return new CompiledLiteral(((Literal) a).value);
                } else if (a instanceof Variable) {
                    return new CompiledAddress(env.resolve((Variable)a));
                } else {
                    throw new CompileFailed("FIXME not implemented argument: " + a.toString());
                }
            };
        }

    }

    private void validateConvention(PassingConvention callConvention, List<LocalEnvironment.Resolution> argumentValues) {
        // adas
    }

    private static String[] doOnShortB(String[] doIt) {
        return list(
                e("if (SpB < 0)", "FIXME compare to stack head"), block(doIt)
        );
    }

    private CompiledCaseCont compileCaseCont(Case _case, PassingConvention stackConvention) {
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

            var alternativesSource = alternatives.entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> compileAlternative(e.getKey(), e.getValue().get(), stackConvention)));

            return new CompiledCaseCont(classEnum, allConstructors, alternativesSource, alternatives);
        } else if (_case.alts[1] instanceof PrimAlt) {
            throw new CompileFailed("FIXME not implemented primitive alternatives: !!! " + _case.toString());
        } else {
            throw new CompileFailed("FIXME not implemented alternatives: !!! " + _case.toString());
        }
    }

    private String[] compileAlternative(String classConstructor, AlgAlt alternative, PassingConvention stackConvention) {
        var nodePassedConvention = new PassingConvention(alternative.cons.args, i -> typeOf(i, alternative.expr).equals(VariableType.Primitive));

        var environment = new LocalEnvironment(stackConvention, nodePassedConvention);
        var altExp = new CompiledExpression(alternative.expr, environment);

        return list(
                comment(
                        "convention:",
                        " - Node contains the constructed closure",
                        " - the other arguments are on the stack!"),

                // pop from stack
                e(popA(stackConvention.pointerVars.length), "pop all passed pointers"),
                e(popB(stackConvention.nonPointerVars.length), "pop all passed non-pointers"),

                altExp.body,
                altExp.jump);
    }

    private <T, R> List<R> select(T[] vals, Class<R> clazz) {
        return Arrays.stream(vals).filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
    }

    private String pushB(String value) {
        return "B[++SpB] = " + value + ";";
    }

    private String popB(int nvars) {
        return "SpB = SpB - " + nvars + ";";
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

    interface Address {
        String code();
    }

    class StackOffset implements Address {
        private final String code;

        public StackOffset(Stack stack, int offset) {
            this.code = stack.name + "[" + stack.ptr + " " + stack.dir + " " + offset + "]";
        }

        @Override
        public String code() {
            return code;
        }
    }

    class NodeOffset implements Address {
        private final String code;

        public NodeOffset(boolean isPointer, int offset) {
            this.code = "Node." + (isPointer ? "pointerWords" : "nonPointerWords") + "[" + offset + "]";
        }

        @Override
        public String code() {
            return code;
        }
    }

    public class LocalEnvironment {
        private final Map<Variable, String> localVarNames;

        private PassedArguments freeVarsIndexMap;
        private PassedArguments boundVarLocations;

//        private final Map<Variable, Address> freeVarsIndexMap;
//        private final Map<Variable, Address> boundVarLocations;

        public LocalEnvironment(PassingConvention boundVars, PassingConvention freeVars) {
            this(
                    new HashMap<>(),
                    new PassedArguments(
                            freeVars,
                            i -> new NodeOffset(true, i),
                            i -> new NodeOffset(false, i)),
                    new PassedArguments(
                            boundVars,
                            i -> new StackOffset(Stack.A, 1 + i),
                            i -> new StackOffset(Stack.B, 1 + i)));
        }

        public LocalEnvironment(Map<Variable, String> localVarNames, PassedArguments freeVarsIndexMap, PassedArguments boundVarLocations) {
            this.localVarNames = localVarNames;
            this.freeVarsIndexMap = freeVarsIndexMap;
            this.boundVarLocations = boundVarLocations;
        }

        public LocalEnvironment rebind(Bind[] newBinds) {
            Map<Variable, String> localVarNames = new HashMap<>();
            for (var bind : newBinds) {
                localVarNames.put(bind.var, "local_" + bind.var.name + "_closure");
            }
            return new LocalEnvironment(localVarNames, freeVarsIndexMap, boundVarLocations);
        }

        public Resolution resolve(Variable f) {
            if (freeVarsIndexMap.has(f)) {
                return new Resolution(freeVarsIndexMap.address(f).code(), ResolutionType.Free, f, freeVarsIndexMap.isPrimitive(f));
            } else if (localVarNames.containsKey(f)) {
                return new Resolution(localVarNames.get(f), ResolutionType.Bound, f, false /* LET does dynamic binding only...*/);
            } else if (boundVarLocations.has(f)) {
                return new Resolution(boundVarLocations.address(f).code(), ResolutionType.Bound, f, boundVarLocations.isPrimitive(f));
            } else {
                ensure(globalBinds.containsKey(f.name), "Can't find variable " + f + " in any environment!");
                return new Resolution(globalClosureName(f), ResolutionType.Global, f, false);
            }
        }

        public StackSave saveToStack() {
            var source = new ArrayList<String>();

            // TODO optimize - remove those vars that won't be needed for evaluation...
            var varsToSave = Stream.of(localVarNames.keySet(), boundVarLocations.variables())
                    .flatMap(Collection::stream).distinct().sorted().toArray(Variable[]::new);

            Map<Variable, String> tempVarNames = IntStream.range(0, varsToSave.length).boxed().collect(Collectors.toMap(i -> varsToSave[i], (i -> "temp$" + i)));

            Function<Variable, String> javaType = var -> resolve(var).isPrimitive ? "int" : "Closure";
            // save to local variables
            source.addAll(List.of(flatten(tempVarNames.entrySet().stream().map(e ->
                    e(javaType.apply(e.getKey()) + " " + e.getValue() + " = (" + javaType.apply(e.getKey()) + ") " + resolve(e.getKey()).resolvedName + ";")))));

            var stackConvention = new PassingConvention(tempVarNames.keySet().toArray(Variable[]::new), var -> resolve(var).isPrimitive);

            // push to stack
            for (var var: stackConvention.pointerVars) {
                source.addAll(List.of(e(pushA(tempVarNames.get(var)))));
            }

            for (var var : stackConvention.nonPointerVars) {
                source.addAll(List.of(e(pushB(tempVarNames.get(var)))));
            }

            ensure(freeVarsIndexMap.isEmpty(), "free var pushing to stack not implemented!!!");

            return new StackSave(source, stackConvention);
        }

        private class StackSave {
            public final List<String> source;
            public final PassingConvention stackConvention;

            public StackSave(ArrayList<String> source, PassingConvention stackConvention) {
                this.source = source;
                this.stackConvention = stackConvention;
            }

        }

        private class Resolution {
            private final String resolvedName;
            private final ResolutionType type;
            private final Variable var;
            private final boolean isPrimitive;

            public Resolution(String resolvedName, ResolutionType type, Variable var, boolean isPrimitive) {
                this.resolvedName = resolvedName;
                this.type = type;
                this.var = var;
                this.isPrimitive = isPrimitive;
            }
        }
    }

}

class CompilationFailed extends RuntimeException {
    public CompilationFailed(String s) {
        super(s);
    }
}
