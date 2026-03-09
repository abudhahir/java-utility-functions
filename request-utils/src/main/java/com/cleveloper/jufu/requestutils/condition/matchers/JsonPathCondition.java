package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Condition that matches values extracted from JSON payloads using JSONPath expressions.
 * Requires JSONPath dependency to be available.
 */
public class JsonPathCondition implements Condition {
    private final String path;
    private final String expectedValue;
    private final MatchOperation operation;
    private final boolean ignoreCase;

    public JsonPathCondition(String path, String expectedValue,
                            MatchOperation operation, boolean ignoreCase) {
        this.path = path;
        this.expectedValue = expectedValue;
        this.operation = operation;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        Object jsonBody = context.getJsonBody();

        if (jsonBody == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonPath")
                    .fieldName(path)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("JSON body is null or not JSON for path '%s'", path))
                    .build()
            );
        }

        String actualValue;
        try {
            Object extractedValue = JsonPath.read(jsonBody.toString(), path);
            actualValue = extractedValue != null ? extractedValue.toString() : null;
        } catch (PathNotFoundException e) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonPath")
                    .fieldName(path)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("JSONPath '%s' not found in body", path))
                    .build()
            );
        } catch (Exception e) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonPath")
                    .fieldName(path)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("Error evaluating JSONPath '%s': %s", path, e.getMessage()))
                    .build()
            );
        }

        if (actualValue == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonPath")
                    .fieldName(path)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("JSONPath '%s' returned null", path))
                    .build()
            );
        }

        boolean matches = StringMatcher.matches(actualValue, expectedValue, operation, ignoreCase);

        if (matches) {
            return ConditionResult.success();
        } else {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonPath")
                    .fieldName(path)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(actualValue)
                    .message(String.format("JSONPath '%s' expected to %s '%s' but was '%s'",
                        path, operation.name().toLowerCase().replace('_', ' '),
                        expectedValue, actualValue))
                    .build()
            );
        }
    }
}
