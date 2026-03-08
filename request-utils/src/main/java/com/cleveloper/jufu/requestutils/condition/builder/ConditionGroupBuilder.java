package com.cleveloper.jufu.requestutils.condition.builder;

import com.cleveloper.jufu.requestutils.condition.core.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for creating ConditionGroup instances.
 * Supports nested groups via andGroup() and orGroup() methods.
 */
public class ConditionGroupBuilder {
    private final List<Condition> conditions = new ArrayList<>();
    private GroupOperator operator = GroupOperator.AND;
    private EvaluationMode mode = EvaluationMode.FAIL_FAST;

    /**
     * Add a condition to be ANDed with others.
     */
    public ConditionGroupBuilder and(Condition condition) {
        if (operator == GroupOperator.OR && !conditions.isEmpty()) {
            throw new IllegalStateException("Cannot mix AND and OR at the same level. Use nested groups.");
        }
        operator = GroupOperator.AND;
        conditions.add(condition);
        return this;
    }

    /**
     * Add a condition to be ORed with others.
     */
    public ConditionGroupBuilder or(Condition condition) {
        if (operator == GroupOperator.AND && !conditions.isEmpty()) {
            throw new IllegalStateException("Cannot mix AND and OR at the same level. Use nested groups.");
        }
        operator = GroupOperator.OR;
        conditions.add(condition);
        return this;
    }

    /**
     * Add a nested AND group.
     */
    public ConditionGroupBuilder andGroup(Consumer<ConditionGroupBuilder> groupConfig) {
        ConditionGroupBuilder nestedBuilder = new ConditionGroupBuilder();
        nestedBuilder.operator = GroupOperator.AND;
        groupConfig.accept(nestedBuilder);
        return and(nestedBuilder.build());
    }

    /**
     * Add a nested OR group.
     */
    public ConditionGroupBuilder orGroup(Consumer<ConditionGroupBuilder> groupConfig) {
        ConditionGroupBuilder nestedBuilder = new ConditionGroupBuilder();
        nestedBuilder.operator = GroupOperator.OR;
        groupConfig.accept(nestedBuilder);
        return and(nestedBuilder.build());
    }

    /**
     * Set the evaluation mode.
     */
    public ConditionGroupBuilder mode(EvaluationMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Build the ConditionGroup.
     */
    public ConditionGroup build() {
        if (conditions.isEmpty()) {
            throw new IllegalStateException("ConditionGroup must have at least one condition");
        }
        return ConditionGroup.of(operator, mode, conditions.toArray(new Condition[0]));
    }
}
