import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.AbstractMachine;
import tea.ExecutionFailed;
import tea.parser.expr.Literal;

import java.io.IOException;

import static parser.ParseTest.lines;
import static tea.parser.Parser.readToGraph;

public class AbstractMachineTest {

    @DataProvider
    public static Object[][] runAbstractMachineCases() throws IOException {
        return new Object[][]{
                {lines("simple_program.stg"), new Literal(42)},
        };
    }

    @Test(dataProvider = "runAbstractMachineCases")
    void runAbstractMachine(String[] lines, Literal expectedResult) throws ExecutionFailed {
        var graph = readToGraph(lines);
        var result = new AbstractMachine(graph).run();
        Assert.assertEquals(result, expectedResult);
    }
}
