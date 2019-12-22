import compilation.CompileJava;
import jas.jasError;
import jasmin.JASMParsingFailed;
import jasmin.MemoryClassLoader;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg2.parser.Parser;
import tea.stg2.parser.ParsingFailed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static parser.StringParserTest.lines;

public class CompileToJavaTest {

    @DataProvider
    public static Object[][] compileCases() throws IOException {
        return new Object[][]{
                {lines("stg2ToJSM/first.stg"), "ReturnInt 42#"},
        };
    }

    @Test(dataProvider = "compileCases")
    void compileToJava(String[] lines, String expectedResult) throws ParsingFailed, JASMParsingFailed, jasError, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var graph = new Parser(lines).graph();
        var javaSource = new Stg2ToJavaCompiler(graph);

        var compiledClassess = Map.of(javaSource.mainClassName(), CompileJava.compile(javaSource.mainClassName(), new ByteArrayInputStream(javaSource.byteArray())));
        var cl = new MemoryClassLoader(compiledClassess);

        runMainOfMain(cl, javaSource.mainClassName());
    }

    private void runMainOfMain(ClassLoader cl, String mainClassName) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class<?> cls = cl.loadClass(mainClassName);
        var args = new Object[]{new String[]{}};
        cls.getMethod("main", String[].class).invoke(null, args);
    }
}

