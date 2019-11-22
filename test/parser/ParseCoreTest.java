package parser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.core.CoreCompiler;
import tea.core.Parser;
import tea.core.ParsingFailed;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;

import java.io.IOException;
import java.util.Arrays;

import static parser.StringParserTest.lines;

public class ParseCoreTest {


    @DataProvider
    public static Object[][] readToGraph() throws IOException {
        return new Object[][]{
                {lines("core/first.c"), "[I`first` I`x` I`y` = V`x`, I`main`  = (V`first` 5 10)]"},
        };
    }

    @DataProvider
    public static Object[][] compileToStg() throws IOException {
        return new Object[][]{
                {lines("core/first.c"), "first = {}\\n{x y} -> x {};\nmain = {}\\n{} -> first {5# 10#};\n"},
                {lines("core/complex_args.c"), "sum = {}\\n{x y} -> + {x y};\n" +
                        "sum3_snd = {}\\n{x y z} -> case sum {x y} of {_hid_0 -> sum {_hid_0 z}};\n" +
                        "main = {}\\n{} -> sum3_snd {5# 8# 29#};\n"},
        };
    }

    @Test(dataProvider = "readToGraph")
    void readToGraph(String[] lines, String expected) throws ParsingFailed {
        var bindings = new Parser(lines).graph();
        Assert.assertEquals(Arrays.toString(bindings), expected);
    }

    @Test(dataProvider = "compileToStg")
    void compileToStg(String[] lines, String expected) throws ParsingFailed {
        var bindings = new Parser(lines).graph();
        var compiled = new CoreCompiler(bindings).compile();
        Assert.assertEquals(compiled, expected);
    }
}
