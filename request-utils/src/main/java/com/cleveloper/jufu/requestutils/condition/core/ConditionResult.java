package com.cleveloper.jufu.requestutils.condition.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of evaluating conditions against a request.
 * Immutable value object.
 */
public class ConditionResult {
    private final boolean matched;
    private final List<ConditionFailure> failures;

    private ConditionResult(boolean matched, List<ConditionFailure> failures) {
        this.matched = matched;
        this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
    }

    public boolean isMatched() {
        return matched;
    }

    public List<ConditionFailure> getFailures() {
        return failures;
    }

    /**
     * Create a successful result with no failures.
     */
    public static ConditionResult success() {
        return new ConditionResult(true, Collections.emptyList());
    }

    /**
     * Create a failed result with a single failure.
     */
    public static ConditionResult failure(ConditionFailure failure) {
        return new ConditionResult(false, List.of(failure));
    }

    /**
     * Create a failed result with multiple failures.
     */
    public static ConditionResult failure(List<ConditionFailure> failures) {
        return new ConditionResult(false, failures);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionResult that = (ConditionResult) o;
        return matched == that.matched && Objects.equals(failures, that.failures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matched, failures);
    }

    @Override
    public String toString() {
        return "ConditionResult{" +
                "matched=" + matched +
                ", failures=" + failures +
                '}';
    }
}
