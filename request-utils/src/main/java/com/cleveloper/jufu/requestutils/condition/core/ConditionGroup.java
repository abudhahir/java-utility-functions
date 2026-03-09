package com.cleveloper.jufu.requestutils.condition.core;

import com.cleveloper.jufu.requestutils.condition.builder.ConditionGroupBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A group of conditions combined with AND or OR logic.
 * Can contain both individual conditions and nested groups.
 */
public class ConditionGroup implements Condition {
    private final GroupOperator operator;
    private final List<Condition> conditions;
    private final EvaluationMode mode;

    private ConditionGroup(GroupOperator operator, List<Condition> conditions, EvaluationMode mode) {
        this.operator = operator;
        this.conditions = conditions;
        this.mode = mode != null ? mode : EvaluationMode.FAIL_FAST;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        return operator == GroupOperator.AND
            ? evaluateAnd(context)
            : evaluateOr(context);
    }

    private ConditionResult evaluateAnd(RequestContext context) {
        List<ConditionFailure> failures = new ArrayList<>();

        for (Condition condition : conditions) {
            ConditionResult result = condition.evaluate(context);

            if (!result.isMatched()) {
                if (mode == EvaluationMode.FAIL_FAST) {
                    return result;
                } else {
                    failures.addAll(result.getFailures());
                }
            }
        }

        return failures.isEmpty()
            ? ConditionResult.success()
            : ConditionResult.failure(failures);
    }

    private ConditionResult evaluateOr(RequestContext context) {
        List<ConditionFailure> failures = new ArrayList<>();

        for (Condition condition : conditions) {
            ConditionResult result = condition.evaluate(context);

            if (result.isMatched()) {
                if (mode == EvaluationMode.FAIL_FAST) {
                    return ConditionResult.success();
                }
                // In COLLECT_ALL mode, continue to evaluate all but return success at end
                return ConditionResult.success();
            } else {
                failures.addAll(result.getFailures());
            }
        }

        return ConditionResult.failure(failures);
    }

    /**
     * Create an AND group from the given conditions.
     */
    public static ConditionGroup and(Condition... conditions) {
        return new ConditionGroup(GroupOperator.AND, Arrays.asList(conditions), null);
    }

    /**
     * Create an OR group from the given conditions.
     */
    public static ConditionGroup or(Condition... conditions) {
        return new ConditionGroup(GroupOperator.OR, Arrays.asList(conditions), null);
    }

    /**
     * Create a group with specified operator and evaluation mode.
     */
    public static ConditionGroup of(GroupOperator operator, EvaluationMode mode, Condition... conditions) {
        return new ConditionGroup(operator, Arrays.asList(conditions), mode);
    }

    /**
     * Create a new builder for fluent construction of condition groups.
     */
    public static ConditionGroupBuilder builder() {
        return new ConditionGroupBuilder();
    }
}
