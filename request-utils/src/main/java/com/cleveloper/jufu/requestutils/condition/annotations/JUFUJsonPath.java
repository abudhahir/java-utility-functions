package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline JSONPath matching configuration.
 * Extracts a value from JSON payload using JSONPath expression and matches it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUJsonPath {

    /**
     * JSONPath expression to extract value (e.g., "$.user.type", "$.items[0].name").
     */
    String path();

    /**
     * Exact value to match (mutually exclusive with other operations).
     */
    String equals() default "";

    /**
     * Substring to match (mutually exclusive with other operations).
     */
    String contains() default "";

    /**
     * Prefix to match (mutually exclusive with other operations).
     */
    String startsWith() default "";

    /**
     * Suffix to match (mutually exclusive with other operations).
     */
    String endsWith() default "";

    /**
     * Regex pattern to match (mutually exclusive with other operations).
     */
    String regex() default "";

    /**
     * Whether to ignore case during matching.
     */
    boolean ignoreCase() default false;
}
