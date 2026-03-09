package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Core interface for all condition types.
 * Conditions evaluate a request context and return a result indicating success or failure.
 */
@FunctionalInterface
public interface Condition {

    /**
     * Evaluate this condition against the given request context.
     *
     * @param context the request context to evaluate
     * @return result indicating whether the condition matched
     */
    ConditionResult evaluate(RequestContext context);
}
