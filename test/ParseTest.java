import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.util.Arrays;

public class ParseTest {

    @DataProvider
    public static Object[][] parseToTokenCases() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "[zero, =, I, 0, ;]"},
                {lines("mid.stg"), "[, foldl, =, FUN(f, acc, list, ->, case, list, of, {, Nil, ->, acc, ;, Cons, " +
                        "h, t, ->, let, {, newAcc, =, THUNK(f, acc, h), }, in, foldl, f, newAcc, t, }), ;, #, lazy, sum," +
                        " with, a, well-known, space, leak, sum, =, FUN(list, ->, foldl, plusInt, zero, list), ;]"},
        };
    }

    @Test(dataProvider = "parseToTokenCases")
    void testParse(String[] lines, String expectedParsed) {
        var tokens = simplify(lines);
        Assert.assertEquals(Arrays.toString(tokens), expectedParsed);
    }

    @DataProvider
    public static Object[][] parseToStringCases() throws IOException {
        return new Object[][]{
                {lines("first.stg"), "zero = I 0 ;"},
                {lines("mid.stg"), " foldl = FUN(f acc list -> case list of { Nil -> acc ; Cons h t -> let {" +
                        " newAcc = THUNK(f acc h) } in foldl f newAcc t }) ; # lazy sum with a well-known space leak sum" +
                        " = FUN(list -> foldl plusInt zero list) ;"},
                {lines("large.stg"), "nil = CON(Nil) ; zero = CON(I 0) ; one = CON(I 1) ; two = CON(I 2) ;" +
                        " three = CON(I 3) ; plusInt = FUN(x y -> case x of { I i -> case y of { I j -> case plus# i j of" +
                        " { x -> let { result = CON (I x) } in result }}}) ; foldl = FUN(f acc list -> case list of { Nil -> acc" +
                        " ; Cons h t -> let { newAcc = THUNK(f acc h) } in foldl f newAcc t }) ; # lazy sum with a well-known" +
                        " space leak sum = FUN(list -> foldl plusInt zero list) ; list1 = CON(Cons one nil) ; list2 = " +
                        "CON(Cons two list1) ; list3 = CON(Cons three list2) ; main = THUNK(sum list3) ;"},
        };
    }

    @Test(dataProvider = "parseToStringCases")
    void testReducedForm(String[] lines, String expectedParsed) {
        var tokens = simplify(lines);
        Assert.assertEquals(String.join(" ", tokens), expectedParsed);
    }


    private String[] simplify(String[] lines) {
        return String.join(" ", lines) // assume space at end of lines
                .replace(";", " ; ") // simplify semicolon tokenization
                .replace('\t', ' ') // simplify whitespaces
                .replaceAll("\\s{2,}", " ") // cleanup whitespaces
                .split("\\s");
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
