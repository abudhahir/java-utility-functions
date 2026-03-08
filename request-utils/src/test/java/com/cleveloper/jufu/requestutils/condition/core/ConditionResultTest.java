package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConditionResultTest {

    @Test
    void shouldCreateSuccessResult() {
        ConditionResult result = ConditionResult.success();

        assertTrue(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldCreateFailureResultWithSingleFailure() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .message("Test failure")
            .build();

        ConditionResult result = ConditionResult.failure(failure);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());
        assertEquals(failure, result.getFailures().get(0));
    }

    @Test
    void shouldCreateFailureResultWithMultipleFailures() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .message("Failure 1")
            .build();
        ConditionFailure failure2 = ConditionFailure.builder()
            .message("Failure 2")
            .build();

        ConditionResult result = ConditionResult.failure(List.of(failure1, failure2));

        assertFalse(result.isMatched());
        assertEquals(2, result.getFailures().size());
    }

    @Test
    void shouldReturnImmutableFailureList() {
        ConditionResult result = ConditionResult.success();

        assertThrows(UnsupportedOperationException.class, () -> {
            result.getFailures().add(ConditionFailure.builder().build());
        });
    }

    @Test
    void shouldReturnImmutableFailureListForFailures() {
        ConditionFailure failure = ConditionFailure.builder()
            .message("Test")
            .build();
        ConditionResult result = ConditionResult.failure(failure);

        assertThrows(UnsupportedOperationException.class, () -> {
            result.getFailures().add(ConditionFailure.builder().build());
        });
    }

    @Test
    void shouldHandleEmptyFailureList() {
        ConditionResult result = ConditionResult.failure(List.of());

        assertFalse(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldBeReflexive() {
        ConditionResult result = ConditionResult.success();

        assertEquals(result, result);
    }

    @Test
    void shouldBeSymmetric() {
        ConditionResult result1 = ConditionResult.success();
        ConditionResult result2 = ConditionResult.success();

        assertEquals(result1, result2);
        assertEquals(result2, result1);
    }

    @Test
    void shouldHaveConsistentHashCode() {
        ConditionFailure failure = ConditionFailure.builder()
            .message("Test")
            .build();
        ConditionResult result1 = ConditionResult.failure(failure);
        ConditionResult result2 = ConditionResult.failure(failure);

        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void shouldNotEqualDifferentMatched() {
        ConditionResult success = ConditionResult.success();
        ConditionResult failure = ConditionResult.failure(
            ConditionFailure.builder().message("Test").build()
        );

        assertNotEquals(success, failure);
    }

    @Test
    void shouldNotEqualDifferentFailures() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .message("Failure 1")
            .build();
        ConditionFailure failure2 = ConditionFailure.builder()
            .message("Failure 2")
            .build();

        ConditionResult result1 = ConditionResult.failure(failure1);
        ConditionResult result2 = ConditionResult.failure(failure2);

        assertNotEquals(result1, result2);
    }

    @Test
    void shouldNotEqualNull() {
        ConditionResult result = ConditionResult.success();

        assertNotEquals(result, null);
    }

    @Test
    void shouldNotEqualDifferentClass() {
        ConditionResult result = ConditionResult.success();

        assertNotEquals(result, "not a ConditionResult");
    }

    @Test
    void shouldProduceValidToString() {
        ConditionResult result = ConditionResult.success();

        String toString = result.toString();

        assertTrue(toString.contains("matched=true"));
        assertTrue(toString.contains("failures="));
    }

    @Test
    void shouldProduceValidToStringForFailure() {
        ConditionFailure failure = ConditionFailure.builder()
            .message("Test failure")
            .build();
        ConditionResult result = ConditionResult.failure(failure);

        String toString = result.toString();

        assertTrue(toString.contains("matched=false"));
        assertTrue(toString.contains("failures="));
    }
}
