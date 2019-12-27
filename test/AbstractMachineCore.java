import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.core.CoreCompiler;
import tea.core.Parser;
import tea.core.ParsingFailed;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;

import java.io.IOException;

import static parser.StringParserTest.lines;

public class AbstractMachineCore {

    @DataProvider
    public static Object[][] executeCompiledStg() throws IOException {
        return new Object[][]{
                {lines("core/first.core"), "ReturnInt 5#"},
                {lines("core/calculation.core"), "ReturnInt 42#"},
                {lines("core/complex_args.core"), "ReturnInt 42#"},
                {lines("core/curried_args.core"), "ReturnInt 42#"},
//                {lines("core/lambda.core"), "ReturnInt 5#"},
        };
    }

    @Test(dataProvider = "executeCompiledStg")
    void executeCompiledStg(String[] lines, String expectedResult)
            throws ParsingFailed, tea.stg2.parser.ParsingFailed, ExecutionFailed {
        var bindings = new Parser(lines).graph();
        var compiled = new CoreCompiler(bindings).compile();
        var graph = new tea.stg2.parser.Parser(new String[]{compiled}).graph();
        var machine = new AbstractMachine(graph);

        System.out.println(machine);

        var result = machine.run();

        Assert.assertEquals(result.toString(), expectedResult);
    }
}
