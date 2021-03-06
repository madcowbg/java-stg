import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg1.machine.AbstractMachine;
import tea.stg1.machine.ExecutionFailed;
import tea.stg1.program.LoadingFailed;
import tea.stg1.program.Program;

import java.io.IOException;

import static parser.StringParserTest.lines;
import static tea.stg1.parser.ParserStg1.readToGraph;

public class AbstractMachineTest {

    @DataProvider
    public static Object[][] runAbstractMachineCases() throws IOException {
        return new Object[][]{
                {lines("stg1/simple_program.stg"), "CON< T[I]  Lit<42>>"},
                {lines("stg1/simple_program_2.stg"), "CON< T[I]  Lit<41>>"},
                {lines("stg1/simple_program_3.stg"), "CON< T[I]  Lit<43>>"},
                {lines("stg1/simple_program_multi_eval.stg"), "CON< T[I]  Lit<42>>"},
                {lines("stg1/simple_program_primitive.stg"), "CON< T[I]  Lit<42>>"},
                {lines("stg1/simple_program_sum3.stg"), "CON< T[I]  Lit<42>>"},
                {lines("stg1/simple_program_sum3boxed.stg"), "CON< T[I]  Lit<42>>"},
        };
    }

    @Test(dataProvider = "runAbstractMachineCases")
    void runAbstractMachine(String[] lines, String expectedResult) throws ExecutionFailed, LoadingFailed {
        var graph = readToGraph(lines);
        var machine = new AbstractMachine(new Program(graph));
        var result = machine.run();
        System.out.println(machine);
        Assert.assertEquals(result.toString(), expectedResult);
    }
}
