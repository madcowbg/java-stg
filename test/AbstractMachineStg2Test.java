import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg2.parser.Parser;
import tea.stg2.parser.ParsingFailed;
import tea.stg2.machine.AbstractMachine;
import tea.stg2.machine.ExecutionFailed;

import java.io.IOException;

import static parser.StringParserTest.lines;

public class AbstractMachineStg2Test {

    @DataProvider
    public static Object[][] runAbstractMachineCases() throws IOException {
        return new Object[][]{
                {lines("stg2/simple_program.stg"), "ReturnCon MkInt 42#"},
                {lines("stg2/simple_program_return_prim.stg"), "ReturnInt 42#"},
                {lines("stg2/simple_program_let_and_case.stg"), "ReturnCon MkInt 42#"},
                {lines("stg2/simple_program_let_and_case_2.stg"), "ReturnInt 42#"},
                {lines("stg2/simple_program_primitive.stg"), "ReturnInt 42#"},
                {lines("stg2/fib_7_rec_exp.stg"), "ReturnInt 21#"},
                {lines("stg2/fib_7_tail.stg"), "ReturnInt 21#"},
                {lines("stg2/partial_app.stg"), "ReturnInt 42#"},
                {lines("stg2/map.stg"), "ReturnCon MkInt 42#"},
                {lines("stg2/curried_f.stg"), "ReturnInt 8#"},
                {lines("stg2/compile_to_stg.stg"), "ReturnInt 12#"},
        };
    }

    @Test(dataProvider = "runAbstractMachineCases")
    void runAbstractMachine(String[] lines, String expectedResult) throws ParsingFailed, ExecutionFailed {
        var graph = new Parser(lines).graph();
        var machine = new AbstractMachine(graph);

        System.out.println(machine);

        var result = machine.run();

        Assert.assertEquals(result.toString(), expectedResult);
    }
}
