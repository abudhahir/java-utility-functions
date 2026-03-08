package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Evaluation mode for condition matching.
 * Determines whether to stop at first failure or collect all failures.
 */
public enum EvaluationMode {
    /**
     * Stop evaluation at the first failed condition (better performance).
     */
    FAIL_FAST,

    /**
     * Evaluate all conditions and collect all failures (better debugging).
     */
    COLLECT_ALL
}
