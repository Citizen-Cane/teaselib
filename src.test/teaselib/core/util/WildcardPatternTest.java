/**
 * 
 */
package teaselib.core.util;

import static org.junit.Assert.*;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * @author someone
 *
 */
public class WildcardPatternTest {

    // http://stackoverflow.com/questions/10664434/escaping-special-characters-in-java-regular-expressions

    static Pattern SPECIAL_REGEX_CHARS = Pattern
            .compile("[{}()\\[\\].+*?^$\\\\|]");

    static String escapeSpecialRegexChars(String str) {
        return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
    }

    static Pattern toSafePattern(String text) {
        return Pattern.compile(".*" + escapeSpecialRegexChars(text) + ".*");
    }

    static Pattern SPECIAL_REGEX_CHARS_JOKER = Pattern
            .compile("[{}()\\[\\].+^$\\\\|]");

    static String[][] jokers = { { "\\*", ".*" }, { "\\?", "." } };

    static String escapeSpecialRegexCharsToJokers(String str) {
        String escaped = SPECIAL_REGEX_CHARS_JOKER.matcher(str)
                .replaceAll("\\\\$0");
        for (String[] joker : jokers) {
            escaped = escaped.replaceAll(joker[0], joker[1]);
        }
        return escaped;
    }

    @Test
    public void testPatternBuilding1() {
        String path = "Pictures/Scene?_*.*";
        Pattern escaped = toSafePattern(path);
        Predicate<String> p = escaped.asPredicate();
        assertEquals(true, p.test(path));
        assertEquals(false, p.test("xxx"));
        assertEquals(false, p.test("Pictures/Scene1_1.jpg"));
    }

    @Test
    public void testJokerPatternBuilding2() {
        String path = escapeSpecialRegexCharsToJokers("Pictures/Scene*_*.*");
        Pattern escaped = Pattern.compile(path);
        Predicate<String> p = escaped.asPredicate();
        assertEquals(false, p.test("xxx"));
        assertEquals(true, p.test("Pictures/Scene1_1.jpg"));
        assertEquals(true, p.test("Pictures/Scene1_12.jpg"));
    }

    @Test
    public void testJokerPatternBuilding3() {
        String path = escapeSpecialRegexCharsToJokers("Pictures/Scene?_*.*");
        Pattern escaped = Pattern.compile(path);
        Predicate<String> p = escaped.asPredicate();
        assertEquals(false, p.test("xxx"));
        assertEquals(true, p.test("Pictures/Scene2_1.jpg"));
        assertEquals(false, p.test("Pictures/Scene22_1.jpg"));
    }

    @Test
    public void testPattern1() {
        final String string = "Pictures/Scene?_*.*";
        Pattern path = WildcardPattern.compile(string);
        Predicate<String> p = path.asPredicate();
        assertEquals(true, p.test(string));
        assertEquals(false, p.test("xxx"));
        assertEquals(true, p.test("Pictures/Scene1_1.jpg"));
    }

    @Test
    public void testJokerPattern2() {
        String string = "Pictures/Scene*_*.*";
        Pattern path = WildcardPattern.compile(string);
        Predicate<String> p = path.asPredicate();
        assertEquals(false, p.test("xxx"));
        assertEquals(true, p.test("Pictures/Scene1_1.jpg"));
        assertEquals(true, p.test("Pictures/Scene1_12.jpg"));
    }

    @Test
    public void testJokerPattern3() {
        String string = "Pictures/Scene?_*.*";
        Pattern path = WildcardPattern.compile(string);
        Predicate<String> p = path.asPredicate();
        assertEquals(false, p.test("xxx"));
        assertEquals(true, p.test("Pictures/Scene2_1.jpg"));
        assertEquals(false, p.test("Pictures/Scene22_1.jpg"));
    }

}
