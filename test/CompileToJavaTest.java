import compilation.CompileJava;
import compilation.Stg2ToJavaCompiler;
import jas.jasError;
import jasmin.JASMParsingFailed;
import jasmin.MemoryClassLoader;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;
import tea.stg2.parser.Parser;
import tea.stg2.parser.ParsingFailed;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import static parser.StringParserTest.lines;

public class CompileToJavaTest {

    @DataProvider
    public static Object[][] compileCases() throws IOException {
        return new Object[][]{
                {"first",  lines("stg2ToJSM/first.stg"), Integer.valueOf(42)},
                {"simple_app",lines("stg2ToJSM/simple_app.stg"), Integer.valueOf(42)},
                {"simple_program_let",lines("stg2ToJSM/simple_program_let.stg"), Integer.valueOf(42)},
                {"simple_program_let_and_case",lines("stg2ToJSM/simple_program_let_and_case.stg"), new Object[]{"MkInt", Integer.valueOf(42)}},
        };
    }

    @Test(dataProvider = "compileCases")
    void compileToJava(String name, String[] lines, Object expectedResult) throws ParsingFailed, IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, ExecutionFailed, InstantiationException {
        var graph = new Parser(lines).graph();

        /* FIXME REMOVE! */
        var machine = new AbstractMachine(graph);
        machine.run();

        var compiler = new Stg2ToJavaCompiler(graph, String.join("\n", lines));
        var javaSource = compiler.compile();

        /* FIXME REMOVE! */
        try(var w = new PrintWriter(new FileOutputStream(new File("generated_" + name + ".java")))) {
            w.println(javaSource);
            w.flush();
        };

        System.out.println(compiler.withLNs());

        var compiledClassess = CompileJava.compile(compiler.mainClassName(), new ByteArrayInputStream(javaSource.getBytes()));
        var cl = new MemoryClassLoader(compiledClassess);


        Class<?> cls = ((ClassLoader) cl).loadClass(compiler.mainClassName());
        var app = cls.getConstructor().newInstance();
        var result = cls.getMethod("eval").invoke(app);

        Assert.assertEquals(expectedResult, result);
    }

}

