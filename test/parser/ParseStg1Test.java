package parser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.stg1.parser.ParserStg1;

import java.io.IOException;
import java.util.Arrays;

import static parser.StringParserTest.lines;

public class ParseStg1Test {

    @DataProvider
    public static Object[][] readToGraph() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "[B<V<zero>, CON< T[I]  Lit<0>>>]"},
                {lines("no_eb.stg"), "[B<V<zero>, CON< T[I]  Lit<0>>>, B<V<fl>, CON< T[I]  Lit<-50>>>, B<V<one>, CON< T[I]  Lit<1>>>]"},
                {lines("fun.stg"), "[B<V<zero>, CON< T[I]  Lit<0>>>, B<V<f>, FUN< V<var1> V<var2> V<var3> -> Lit<0>>>]"},
                {lines("fact.stg"), "[B<V<fact_dummy>, FUN< V<val> -> case <V<val>, Alts<DC<T[I] V<sth> -> V<other>>;Def<V<x> -> Lit<1>>>>>>]"},
                {lines("constructor_cases.stg"), "[B<V<asd>, CON< T[I]  Lit<1>>>, B<V<f>, CON< T[I]  V<asd>>>]"},
//                {lines("large.stg"), ""},
        };
    }

    @Test(dataProvider = "readToGraph")
    void readToGraph(String[] lines, String expected) {
        var bindings = ParserStg1.readToGraph(lines);
        Assert.assertEquals(Arrays.toString(bindings), expected);
    }

    @DataProvider
    public static Object[][] borderCaseFails() throws IOException {
        return new Object[][]{
                {lines("border_cases/kwd_instd_var.stg")},
                {lines("border_cases/kwd_instd_lit.stg")},
        };
    }

    @Test(dataProvider = "borderCaseFails", expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "T\\[of\\] is not.*")
    void checkBorderCaseFails(String[] lines) {
        ParserStg1.readToGraph(lines);
    }
}
