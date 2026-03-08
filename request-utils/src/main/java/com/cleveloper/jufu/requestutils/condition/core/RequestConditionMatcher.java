package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Main service for evaluating conditions against HTTP requests.
 * Framework-agnostic - operates on the Condition interface and RequestContext abstraction.
 */
public class RequestConditionMatcher {

    /**
     * Evaluate a condition against an HTTP request.
     *
     * @param condition the condition to evaluate
     * @param request the HTTP request
     * @return the evaluation result
     */
    public ConditionResult evaluate(Condition condition, HttpServletRequest request) {
        RequestContext context = RequestContext.from(request);
        return condition.evaluate(context);
    }

    /**
     * Evaluate a condition against a request context.
     *
     * @param condition the condition to evaluate
     * @param context the request context
     * @return the evaluation result
     */
    public ConditionResult evaluate(Condition condition, RequestContext context) {
        return condition.evaluate(context);
    }
}
