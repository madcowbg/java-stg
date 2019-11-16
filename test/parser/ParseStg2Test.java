package parser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.parserStg2.ParserStg2;
import tea.parserStg2.ParsingFailed;

import java.io.IOException;
import java.util.Arrays;

import static parser.StringParserTest.lines;

public class ParseStg2Test {


    @DataProvider
    public static Object[][] readToGraph() throws IOException {
        return new Object[][]{
                {lines("stg2/simple_program.stg"), "[asd = {} \\n {} -> MkInt {#`1`}, f = {asd} \\n {} -> MkInt {asd}, answ_42 = {} \\n {} -> MkInt {#`42`}, main = {} \\n {} -> answ_42 {}, nil = {} \\n {} -> Nil {}, singleton = {} \\n {x} -> Cons {x,nil}, ANSWER = {} \\n {} -> #`42`]"},
        };
    }

    @Test(dataProvider = "readToGraph")
    void readToGraph(String[] lines, String expected) throws ParsingFailed {
        var bindings = new ParserStg2(lines).graph();
        Assert.assertEquals(Arrays.toString(bindings), expected);
    }
}
