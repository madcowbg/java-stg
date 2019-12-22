import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;
import tea.stg2.parser.Parser;
import tea.stg2.parser.ParsingFailed;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
    void runAbstractMachine(String[] lines, String expectedResult) throws ParsingFailed, ExecutionFailed {
        var graph = new Parser(lines).graph();
        var machine = new AbstractMachine(graph);

        System.out.println(machine);

        var result = machine.run();

        Assert.assertEquals(result.toString(), expectedResult);
    }

    @Test
    void runReadyClassFile() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var className = "Main";
        Map<String, byte[]> classes = readClassess(className);

        // Create a new class loader with the directory
        ClassLoader cl = new ClassLoader() {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };

        Class<?> cls = cl.loadClass("Main");
        var args = new Object[]{new String[]{}};
        cls.getMethod("main", String[].class).invoke(null, args);
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
}
