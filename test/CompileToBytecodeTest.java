import jas.jasError;
import jasmin.CompileJASM;
import jasmin.JASMParsingFailed;
import jasmin.MemoryClassLoader;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;
import tea.stg2.parser.Parser;
import tea.stg2.parser.ParsingFailed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static parser.StringParserTest.lines;

public class CompileToBytecodeTest {

    @DataProvider
    public static Object[][] compileCases() throws IOException {
        return new Object[][]{
                {lines("stg2ToJSM/first.stg"), "ReturnInt 42#"},
        };
    }

    @Test(dataProvider = "compileCases")
    void runAbstractMachine(String[] lines, String expectedResult) throws ParsingFailed, ExecutionFailed, JASMParsingFailed, jasError, IOException {
        var graph = new Parser(lines).graph();
        var machine = new AbstractMachine(graph);

        System.out.println(machine);

        var result = machine.run();

        Assert.assertEquals(result.toString(), expectedResult);
    }

    @Test(dataProvider = "compileCases")
    void compileToBytecode(String[] lines, String expectedResult) throws ParsingFailed, JASMParsingFailed, jasError, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var graph = new Parser(lines).graph();

        var jasmSource = new Stg2Compiler(graph);
        var compiledClassess = Map.of(jasmSource.mainClassName(), CompileJASM.assemble("someapp", new ByteArrayInputStream(jasmSource.byteArray())));

        var cl = new MemoryClassLoader(compiledClassess);
        runMainOfMain(cl, jasmSource.mainClassName());
    }


    @Test
    void runReadyClassFile() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Map<String, byte[]> classes = readClassess("Main");

        ClassLoader cl = new MemoryClassLoader(classes);

        runMainOfMain(cl, "Main");
    }

    private Map<String, byte[]> readClassess(String... className) {
        return Stream.of(className)
                .collect(Collectors.toMap(
                        Function.identity(),
                        cn -> readResource("classes/" + cn + ".clazz")));
    }

    private byte[] readResource(String resource) {
        try {
            return CompileToBytecodeTest.class.getResourceAsStream(resource).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void compileAndRunClassFile() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<String> classNames = List.of("Main");
        Map<String, byte[]> asmFiles = classNames.stream().collect(Collectors.toMap(
                Function.identity(),
                f -> readResource("classes/" + f + ".j")));

        Map<String, byte[]> compiled = asmFiles.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    try {
                        return CompileJASM.assemble(e.getKey(), new ByteArrayInputStream(e.getValue()));
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }));

        ClassLoader cl = new MemoryClassLoader(compiled);

        runMainOfMain(cl, "Main");
    }

    private void runMainOfMain(ClassLoader cl, String mainClassName) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class<?> cls = cl.loadClass(mainClassName);
        var args = new Object[]{new String[]{}};
        cls.getMethod("main", String[].class).invoke(null, args);
    }
}

