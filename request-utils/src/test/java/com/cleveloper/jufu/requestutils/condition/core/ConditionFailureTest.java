package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConditionFailure value object.
 * Validates builder pattern, immutability, and field handling.
 */
class ConditionFailureTest {

    @Test
    void shouldCreateFailureWithAllFields() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Header 'X-Api-Key' expected to equal 'premium' but was 'basic'")
            .build();

        assertEquals("Header", failure.getConditionType());
        assertEquals("X-Api-Key", failure.getFieldName());
        assertEquals("equals", failure.getOperation());
        assertEquals("premium", failure.getExpectedValue());
        assertEquals("basic", failure.getActualValue());
        assertTrue(failure.getMessage().contains("premium"));
    }

    @Test
    void shouldGenerateDescriptiveToString() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .message("Test message")
            .build();

        String result = failure.toString();
        assertTrue(result.contains("Header"));
        assertTrue(result.contains("Test message"));
    }

    @Test
    void shouldCreateFailureWithMinimalFields() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("QueryParam")
            .message("Validation failed")
            .build();

        assertEquals("QueryParam", failure.getConditionType());
        assertEquals("Validation failed", failure.getMessage());
        assertNull(failure.getFieldName());
        assertNull(failure.getOperation());
        assertNull(failure.getExpectedValue());
        assertNull(failure.getActualValue());
    }

    @Test
    void shouldHandleNullFieldsGracefully() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("PathVariable")
            .fieldName(null)
            .operation(null)
            .expectedValue(null)
            .actualValue(null)
            .message("Error occurred")
            .build();

        assertNull(failure.getFieldName());
        assertNull(failure.getOperation());
        assertNull(failure.getExpectedValue());
        assertNull(failure.getActualValue());
        assertEquals("PathVariable", failure.getConditionType());
        assertEquals("Error occurred", failure.getMessage());
    }

    @Test
    void shouldBeImmutable() {
        ConditionFailure.Builder builder = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Custom");

        ConditionFailure failure1 = builder.build();

        // Modify builder after first build
        builder.fieldName("X-Modified");
        ConditionFailure failure2 = builder.build();

        // First instance should remain unchanged
        assertEquals("X-Custom", failure1.getFieldName());
        assertEquals("X-Modified", failure2.getFieldName());
    }

    @Test
    void shouldIncludeAllFieldsInToString() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("Authorization")
            .operation("contains")
            .expectedValue("Bearer")
            .actualValue("Basic")
            .message("Authorization header invalid")
            .build();

        String result = failure.toString();
        assertTrue(result.contains("Header"), "toString should contain conditionType");
        assertTrue(result.contains("Authorization"), "toString should contain fieldName");
        assertTrue(result.contains("contains"), "toString should contain operation");
        assertTrue(result.contains("Bearer"), "toString should contain expectedValue");
        assertTrue(result.contains("Basic"), "toString should contain actualValue");
        assertTrue(result.contains("Authorization header invalid"), "toString should contain message");
    }

    @Test
    void shouldCreateDistinctInstancesFromBuilder() {
        ConditionFailure.Builder builder = ConditionFailure.builder()
            .conditionType("QueryParam")
            .message("Test");

        ConditionFailure failure1 = builder.build();
        ConditionFailure failure2 = builder.build();

        assertNotSame(failure1, failure2, "Builder should create distinct instances");
        assertEquals(failure1.getConditionType(), failure2.getConditionType());
        assertEquals(failure1.getMessage(), failure2.getMessage());
    }
}
