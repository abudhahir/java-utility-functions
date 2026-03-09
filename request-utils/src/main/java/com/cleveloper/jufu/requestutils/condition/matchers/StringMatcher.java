package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.MatchOperation;
import java.util.regex.Pattern;

/**
 * Utility for matching strings based on various operations.
 */
public class StringMatcher {

    /**
     * Check if the actual value matches the expected value using the specified operation.
     *
     * @param actualValue the actual value from the request
     * @param expectedValue the expected value to match against
     * @param operation the matching operation to perform
     * @param ignoreCase whether to perform case-insensitive matching
     * @return true if the values match according to the operation
     */
    public static boolean matches(String actualValue, String expectedValue,
                                  MatchOperation operation, boolean ignoreCase) {
        if (actualValue == null) {
            return false;
        }

        String actual = ignoreCase ? actualValue.toLowerCase() : actualValue;
        String expected = ignoreCase ? expectedValue.toLowerCase() : expectedValue;

        return switch (operation) {
            case EQUALS -> actual.equals(expected);
            case CONTAINS -> actual.contains(expected);
            case STARTS_WITH -> actual.startsWith(expected);
            case ENDS_WITH -> actual.endsWith(expected);
            case REGEX -> matchesRegex(actualValue, expectedValue, ignoreCase);
        };
    }

    private static boolean matchesRegex(String actualValue, String pattern, boolean ignoreCase) {
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        return Pattern.compile(pattern, flags).matcher(actualValue).matches();
    }
}
