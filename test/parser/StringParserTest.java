package parser;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Optional;

import static tea.parser.Tokenizer.tokenize;

public class StringParserTest {
    @DataProvider
    public static Object[][] parseToTokenCases() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "[zero, =, CON(, I, 0, ), ;]"},
                {lines("mid.stg"), "[foldl, =, FUN(, f, acc, list, ->, case, list, of, {, Nil, ->, acc, ;, Cons, h, t, ->, let, {, newAcc, =, THUNK(, f, acc, h, ), }, in, foldl, f, newAcc, t, }, ), ;, sum, =, FUN(, list, ->, foldl, plusInt, zero, list, )]"},
        };
    }

    @Test(dataProvider = "parseToTokenCases")
    void testParse(String[] lines, String expectedParsed) {
        var tokens = tokenize(lines);
        Assert.assertEquals(Arrays.toString(tokens), expectedParsed);
    }

    @DataProvider
    public static Object[][] parseToStringCases() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "zero = CON( I 0 ) ;"},
                {lines("mid.stg"), "foldl = FUN( f acc list -> case list of { Nil -> acc ; Cons h t -> let { newAcc = THUNK( f acc h ) } in foldl f newAcc t } ) ; sum = FUN( list -> foldl plusInt zero list )"},
                {lines("large.stg"), "nil = CON( Nil ) ; zero = CON( I 0 ) ; one = CON( I 1 ) ; two = CON( I 2 ) ; three = CON( I 3 ) ; plusInt = FUN( x y -> case x of { I i -> case y of { I j -> case plus# i j of { x -> let { result = CON ( I x ) } in result } } } ) ; foldl = FUN( f acc list -> case list of { Nil -> acc ; Cons h t -> let { newAcc = THUNK( f acc h ) } in foldl f newAcc t } ) ; sum = FUN( list -> foldl plusInt zero list ) ; list1 = CON( Cons one nil ) ; list2 = CON( Cons two list1 ) ; list3 = CON( Cons three list2 ) ; main = THUNK( sum list3 ) ;"},
        };
    }

    @Test(dataProvider = "parseToStringCases")
    void testReducedForm(String[] lines, String expectedParsed) {
        var tokens = tokenize(lines);
        Assert.assertEquals(String.join(" ", tokens), expectedParsed);
    }


    public static String[] lines(String path) throws IOException {
        try (var reader = resourceStream(path)) {
            return new BufferedReader(new InputStreamReader(reader)).lines().toArray(String[]::new);
        }
    }

    private static InputStream resourceStream(String path) {
        return Optional.ofNullable(ParseStg2Test.class.getResourceAsStream(path))
                .orElseGet(() -> {throw new RuntimeException(path);});
    }
}
