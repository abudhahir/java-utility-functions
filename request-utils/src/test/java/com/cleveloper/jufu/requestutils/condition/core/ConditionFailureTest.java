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

    @Test
    void shouldSatisfyReflexivePropertyOfEquals() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        assertEquals(failure, failure, "Object should equal itself (reflexive property)");
    }

    @Test
    void shouldSatisfySymmetricPropertyOfEquals() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        ConditionFailure failure2 = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        assertEquals(failure1, failure2, "x.equals(y) should return true");
        assertEquals(failure2, failure1, "y.equals(x) should return true (symmetric property)");
    }

    @Test
    void shouldHaveEqualHashCodesForEqualObjects() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        ConditionFailure failure2 = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        assertEquals(failure1, failure2, "Objects should be equal");
        assertEquals(failure1.hashCode(), failure2.hashCode(), "Equal objects must have equal hashCodes");
    }

    @Test
    void shouldNotBeEqualToDifferentObjects() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        ConditionFailure failure2 = ConditionFailure.builder()
            .conditionType("QueryParam")
            .fieldName("apiKey")
            .operation("contains")
            .expectedValue("gold")
            .actualValue("silver")
            .message("Different message")
            .build();

        assertNotEquals(failure1, failure2, "Different objects should not be equal");
    }

    @Test
    void shouldReturnFalseWhenComparedToNull() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .fieldName("X-Api-Key")
            .operation("equals")
            .expectedValue("premium")
            .actualValue("basic")
            .message("Test message")
            .build();

        assertNotEquals(failure, null, "Object should not equal null");
        assertNotEquals(null, failure, "null should not equal object");
    }

    @Test
    void shouldGenerateToStringWithAllNullFields() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType(null)
            .fieldName(null)
            .operation(null)
            .expectedValue(null)
            .actualValue(null)
            .message(null)
            .build();

        String result = failure.toString();
        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("ConditionFailure"), "toString should contain class name");
        assertTrue(result.contains("conditionType="), "toString should contain conditionType field");
        assertTrue(result.contains("fieldName="), "toString should contain fieldName field");
        assertTrue(result.contains("operation="), "toString should contain operation field");
        assertTrue(result.contains("expectedValue="), "toString should contain expectedValue field");
        assertTrue(result.contains("actualValue="), "toString should contain actualValue field");
        assertTrue(result.contains("message="), "toString should contain message field");
    }
}
