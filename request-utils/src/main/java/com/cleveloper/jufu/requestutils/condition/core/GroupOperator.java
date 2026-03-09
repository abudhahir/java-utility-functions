package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Logical operators for combining conditions in groups.
 */
public enum GroupOperator {
    /**
     * All conditions must match (logical AND).
     */
    AND,

    /**
     * At least one condition must match (logical OR).
     */
    OR
}
