import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.AbstractMachine;
import tea.ExecutionFailed;
import tea.LoadingFailed;
import tea.Program;

import java.io.IOException;

import static parser.ParseTest.lines;
import static tea.parser.Parser.readToGraph;

public class AbstractMachineTest {

    @DataProvider
    public static Object[][] runAbstractMachineCases() throws IOException {
        return new Object[][]{
                {lines("simple_program.stg"), "CON< T[I]  Lit<42>>"},
                {lines("simple_program_2.stg"), "CON< T[I]  Lit<41>>"},
                {lines("simple_program_3.stg"), "CON< T[I]  Lit<43>>"},
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
