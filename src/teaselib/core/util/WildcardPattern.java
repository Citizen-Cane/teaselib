/**
 * 
 */
package teaselib.core.util;

import java.util.regex.Pattern;

/**
 * A simple pattern matching strings. The wildcard characters are "*" to match
 * any number of characters, and the "?" to match exactly one character.
 */
public class WildcardPattern {

    private static Pattern SPECIAL_REGEX_CHARS_JOKER = Pattern
            .compile("[{}()\\[\\].+^$\\\\|]");

    private static String[][] wildcards = { { "\\*", ".*" }, { "\\?", "." } };

    private static String convertWildcardsToRegex(String str) {
        String escaped = SPECIAL_REGEX_CHARS_JOKER.matcher(str)
                .replaceAll("\\\\$0");
        for (String[] joker : wildcards) {
            escaped = escaped.replaceAll(joker[0], joker[1]);
        }
        return escaped;
    }

    /**
     * Compiles a wildcard string into a pattern, which can be used for
     * matching.
     * 
     * @param wildcardPattern
     *            A string with wildcards as described in the class description.
     * @return A regex pattern that can be used to match strings.
     */
    public static Pattern compile(String wildcardPattern) {
        return Pattern.compile(convertWildcardsToRegex(wildcardPattern));
    }
}