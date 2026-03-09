package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;

/**
 * Condition that matches HTTP header values.
 */
public class HeaderCondition implements Condition {
    private final String headerName;
    private final String expectedValue;
    private final MatchOperation operation;
    private final boolean ignoreCase;

    public HeaderCondition(String headerName, String expectedValue,
                          MatchOperation operation, boolean ignoreCase) {
        this.headerName = headerName;
        this.expectedValue = expectedValue;
        this.operation = operation;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        String actualValue = context.getHeader(headerName);

        if (actualValue == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("Header")
                    .fieldName(headerName)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("Header '%s' is missing", headerName))
                    .build()
            );
        }

        boolean matches = StringMatcher.matches(actualValue, expectedValue, operation, ignoreCase);

        if (matches) {
            return ConditionResult.success();
        } else {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("Header")
                    .fieldName(headerName)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(actualValue)
                    .message(String.format("Header '%s' expected to %s '%s' but was '%s'",
                        headerName, operation.name().toLowerCase().replace('_', ' '),
                        expectedValue, actualValue))
                    .build()
            );
        }
    }
}
