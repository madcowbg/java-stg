import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;
import tea.stg2.parser.Parser;
import tea.stg2.parser.ParsingFailed;

import java.io.IOException;

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
}
