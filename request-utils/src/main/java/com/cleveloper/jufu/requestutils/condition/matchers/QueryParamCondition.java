package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;

/**
 * Condition that matches query parameter values.
 */
public class QueryParamCondition implements Condition {
    private final String paramName;
    private final String expectedValue;
    private final MatchOperation operation;
    private final boolean ignoreCase;

    public QueryParamCondition(String paramName, String expectedValue,
                              MatchOperation operation, boolean ignoreCase) {
        this.paramName = paramName;
        this.expectedValue = expectedValue;
        this.operation = operation;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        String actualValue = context.getQueryParam(paramName);

        if (actualValue == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("QueryParam")
                    .fieldName(paramName)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("Query parameter '%s' is missing", paramName))
                    .build()
            );
        }

        boolean matches = StringMatcher.matches(actualValue, expectedValue, operation, ignoreCase);

        if (matches) {
            return ConditionResult.success();
        } else {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("QueryParam")
                    .fieldName(paramName)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(actualValue)
                    .message(String.format("Query parameter '%s' expected to %s '%s' but was '%s'",
                        paramName, operation.name().toLowerCase().replace('_', ' '),
                        expectedValue, actualValue))
                    .build()
            );
        }
    }
}
