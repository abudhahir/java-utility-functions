package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline query parameter matching configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUQueryParam {

    /**
     * Query parameter name to match.
     */
    String name();

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
