package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses @JUFU annotations into Condition objects.
 * Converts annotation-based configuration into executable condition instances.
 */
public class AnnotationConditionParser {

    /**
     * Parse an array of @JUFUCondition annotations into a ConditionGroup.
     * Multiple conditions are combined with AND logic.
     *
     * @param annotations array of condition annotations
     * @param mode evaluation mode for the group
     * @return ConditionGroup representing all conditions
     */
    public ConditionGroup parse(JUFUCondition[] annotations, EvaluationMode mode) {
        List<Condition> conditions = new ArrayList<>();

        for (JUFUCondition annotation : annotations) {
            Condition condition = parseCondition(annotation);
            if (condition != null) {
                conditions.add(condition);
            }
        }

        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("No valid conditions found in annotations");
        }

        return ConditionGroup.of(GroupOperator.AND, mode, conditions.toArray(new Condition[0]));
    }

    /**
     * Parse a single @JUFUCondition annotation into a Condition.
     * Supports both class reference mode and inline mode.
     *
     * @param annotation the condition annotation
     * @return Condition instance, or null if annotation is empty
     */
    private Condition parseCondition(JUFUCondition annotation) {
        List<Condition> conditions = new ArrayList<>();

        // Check for class reference mode
        if (annotation.value() != null && annotation.value() != Condition.class) {
            try {
                conditions.add(annotation.value().getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "Failed to instantiate condition class: " + annotation.value().getName(), e
                );
            }
        }

        // Check for inline header condition
        JUFUHeader header = annotation.header();
        if (header != null && !header.name().isEmpty()) {
            conditions.add(parseHeader(header));
        }

        // Check for inline query param condition
        JUFUQueryParam queryParam = annotation.queryParam();
        if (queryParam != null && !queryParam.name().isEmpty()) {
            conditions.add(parseQueryParam(queryParam));
        }

        // Check for inline JSON path condition
        JUFUJsonPath jsonPath = annotation.jsonPath();
        if (jsonPath != null && !jsonPath.path().isEmpty()) {
            conditions.add(parseJsonPath(jsonPath));
        }

        // Check for inline JSON exact match condition
        JUFUJsonExactMatch jsonExactMatch = annotation.jsonExactMatch();
        if (jsonExactMatch != null && !jsonExactMatch.template().isEmpty()) {
            conditions.add(parseJsonExactMatch(jsonExactMatch));
        }

        // If multiple inline conditions are present, combine them with AND
        if (conditions.isEmpty()) {
            return null;
        } else if (conditions.size() == 1) {
            return conditions.get(0);
        } else {
            return ConditionGroup.and(conditions.toArray(new Condition[0]));
        }
    }

    /**
     * Parse a @JUFUHeader annotation into a HeaderCondition.
     */
    private Condition parseHeader(JUFUHeader header) {
        String name = header.name();
        MatchOperation operation = determineOperation(
            header.equals(), header.contains(), header.startsWith(),
            header.endsWith(), header.regex()
        );
        String expectedValue = getOperationValue(
            operation, header.equals(), header.contains(), header.startsWith(),
            header.endsWith(), header.regex()
        );

        return new HeaderCondition(name, expectedValue, operation, header.ignoreCase());
    }

    /**
     * Parse a @JUFUQueryParam annotation into a QueryParamCondition.
     */
    private Condition parseQueryParam(JUFUQueryParam queryParam) {
        String name = queryParam.name();
        MatchOperation operation = determineOperation(
            queryParam.equals(), queryParam.contains(), queryParam.startsWith(),
            queryParam.endsWith(), queryParam.regex()
        );
        String expectedValue = getOperationValue(
            operation, queryParam.equals(), queryParam.contains(), queryParam.startsWith(),
            queryParam.endsWith(), queryParam.regex()
        );

        return new QueryParamCondition(name, expectedValue, operation, queryParam.ignoreCase());
    }

    /**
     * Parse a @JUFUJsonPath annotation into a JsonPathCondition.
     */
    private Condition parseJsonPath(JUFUJsonPath jsonPath) {
        String path = jsonPath.path();
        MatchOperation operation = determineOperation(
            jsonPath.equals(), jsonPath.contains(), jsonPath.startsWith(),
            jsonPath.endsWith(), jsonPath.regex()
        );
        String expectedValue = getOperationValue(
            operation, jsonPath.equals(), jsonPath.contains(), jsonPath.startsWith(),
            jsonPath.endsWith(), jsonPath.regex()
        );

        return new JsonPathCondition(path, expectedValue, operation, jsonPath.ignoreCase());
    }

    /**
     * Parse a @JUFUJsonExactMatch annotation into a JsonExactMatchCondition.
     */
    private Condition parseJsonExactMatch(JUFUJsonExactMatch jsonExactMatch) {
        return new JsonExactMatchCondition(jsonExactMatch.template(), jsonExactMatch.fields());
    }

    /**
     * Determine which MatchOperation is specified based on which attribute is non-empty.
     * Only one operation should be specified at a time.
     */
    private MatchOperation determineOperation(String equals, String contains, String startsWith,
                                             String endsWith, String regex) {
        int count = 0;
        MatchOperation operation = null;

        if (!equals.isEmpty()) {
            count++;
            operation = MatchOperation.EQUALS;
        }
        if (!contains.isEmpty()) {
            count++;
            operation = MatchOperation.CONTAINS;
        }
        if (!startsWith.isEmpty()) {
            count++;
            operation = MatchOperation.STARTS_WITH;
        }
        if (!endsWith.isEmpty()) {
            count++;
            operation = MatchOperation.ENDS_WITH;
        }
        if (!regex.isEmpty()) {
            count++;
            operation = MatchOperation.REGEX;
        }

        if (count == 0) {
            throw new IllegalArgumentException("No match operation specified in annotation");
        }
        if (count > 1) {
            throw new IllegalArgumentException("Multiple match operations specified in annotation - only one is allowed");
        }

        return operation;
    }

    /**
     * Get the value for the determined operation.
     */
    private String getOperationValue(MatchOperation operation, String equals, String contains,
                                     String startsWith, String endsWith, String regex) {
        return switch (operation) {
            case EQUALS -> equals;
            case CONTAINS -> contains;
            case STARTS_WITH -> startsWith;
            case ENDS_WITH -> endsWith;
            case REGEX -> regex;
        };
    }
}
