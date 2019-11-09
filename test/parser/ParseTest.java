package parser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tea.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ParseTest {

    @DataProvider
    public static Object[][] parseToTokenCases() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "[zero, =, CON(, I, 0, ), ;]"},
                {lines("mid.stg"), "[foldl, =, FUN(, f, acc, list, ->, case, list, of, {, Nil, ->, acc, ;, Cons, h, t, ->, let, {, newAcc, =, THUNK(, f, acc, h, ), }, in, foldl, f, newAcc, t, }, ), ;, sum, =, FUN(, list, ->, foldl, plusInt, zero, list, )]"},
        };
    }

    @Test(dataProvider = "parseToTokenCases")
    void testParse(String[] lines, String expectedParsed) {
        var tokens = Parser.tokenize(lines);
        Assert.assertEquals(Arrays.toString(tokens), expectedParsed);
    }

    @DataProvider
    public static Object[][] parseToStringCases() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "zero = CON( I 0 ) ;"},
                {lines("mid.stg"), "foldl = FUN( f acc list -> case list of { Nil -> acc ; Cons h t -> let { newAcc = THUNK( f acc h ) } in foldl f newAcc t } ) ; sum = FUN( list -> foldl plusInt zero list )"},
                {lines("large.stg"), "nil = CON( Nil ) ; zero = CON( I 0 ) ; one = CON( I 1 ) ; two = CON( I 2 ) ; three = CON( I 3 ) ; plusInt = FUN( x y -> case x of { I i -> case y of { I j -> case plus# i j of { x -> let { result = CON ( I x ) } in result }}} ) ; foldl = FUN( f acc list -> case list of { Nil -> acc ; Cons h t -> let { newAcc = THUNK( f acc h ) } in foldl f newAcc t } ) ; sum = FUN( list -> foldl plusInt zero list ) ; list1 = CON( Cons one nil ) ; list2 = CON( Cons two list1 ) ; list3 = CON( Cons three list2 ) ; main = THUNK( sum list3 ) ;"},
        };
    }

    @Test(dataProvider = "parseToStringCases")
    void testReducedForm(String[] lines, String expectedParsed) {
        var tokens = Parser.tokenize(lines);
        Assert.assertEquals(String.join(" ", tokens), expectedParsed);
    }

    @DataProvider
    public static Object[][] readToGraph() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "[B<V<zero>, CONS< T[I]  A<0>>>]"},
                {lines("no_eb.stg"), "[B<V<zero>, CONS< T[I]  A<0>>>, B<V<fl>, CONS< T[I]  A<-50>>>, B<V<one>, CONS< T[I]  A<1>>>]"},
                {lines("fun.stg"), "[B<V<zero>, CONS< T[I]  A<0>>>, B<V<f>, FUN< V<var1> V<var2> V<var3> -> A<0>>>]"},
//                {lines("mid.stg"), ""},
//                {lines("large.stg"), ""},
        };
    }

    @Test(dataProvider = "readToGraph")
    void readToGraph(String[] lines, String expected) {
        var bindings = Parser.readToGraph(lines);
        Assert.assertEquals(Arrays.toString(bindings), expected);
    }

    private static BufferedReader resourceStream(String path) {
        return new BufferedReader(new InputStreamReader(ParseTest.class.getResourceAsStream(path)));
    }

    private static String[] lines(String path) throws IOException {
        try (var reader = resourceStream(path)) {
            return reader.lines().toArray(String[]::new);
        }
    }
}
