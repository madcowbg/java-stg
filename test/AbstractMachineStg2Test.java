import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.parserStg2.ParserStg2;
import tea.parserStg2.ParsinFailed;
import tea.parserStg2.machine.AbstractMachine;
import tea.parserStg2.machine.ExecutionFailed;

import java.io.IOException;

import static parser.StringParserTest.lines;

public class AbstractMachineStg2Test {

    @DataProvider
    public static Object[][] runAbstractMachineCases() throws IOException {
        return new Object[][]{
                {lines("stg2/simple_program.stg"), "MkInt {#`42`}"},
                {lines("stg2/simple_program_return_prim.stg"), "42#"},
                {lines("stg2/simple_program_let_and_case.stg"), "MkInt {#`42`}"},
        };
    }

    @Test(dataProvider = "runAbstractMachineCases")
    void runAbstractMachine(String[] lines, String expectedResult) throws ParsinFailed, ExecutionFailed {
        var graph = new ParserStg2(lines).graph();
        var machine = new AbstractMachine(graph);

        System.out.println(machine);

        var result = machine.run();

        Assert.assertEquals(result.toString(), expectedResult);
    }
}
