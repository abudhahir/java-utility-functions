package com.cleveloper.jufu.requestutils.condition.core;

/**
 * String matching operations supported by conditions.
 */
public enum MatchOperation {
    /**
     * Exact string match (case-sensitive).
     */
    EQUALS,

    /**
     * Checks if the value contains the specified substring.
     */
    CONTAINS,

    /**
     * Checks if the value starts with the specified prefix.
     */
    STARTS_WITH,

    /**
     * Checks if the value ends with the specified suffix.
     */
    ENDS_WITH,

    /**
     * Matches the value against a regular expression pattern.
     */
    REGEX
}
