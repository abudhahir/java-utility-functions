package com.cleveloper.jufu.requestutils.condition.core;

import java.util.Objects;

/**
 * Value object representing a condition validation failure.
 * Contains detailed information about why a condition check failed,
 * useful for debugging and error reporting.
 *
 * <p>This class is immutable and uses the builder pattern for construction.
 * All fields are optional except where business logic requires them.
 *
 * <p>Example usage:
 * <pre>{@code
 * ConditionFailure failure = ConditionFailure.builder()
 *     .conditionType("Header")
 *     .fieldName("X-Api-Key")
 *     .operation("equals")
 *     .expectedValue("premium")
 *     .actualValue("basic")
 *     .message("Header 'X-Api-Key' expected to equal 'premium' but was 'basic'")
 *     .build();
 * }</pre>
 *
 * @since 0.0.1
 */
public final class ConditionFailure {

    private final String conditionType;
    private final String fieldName;
    private final String operation;
    private final String expectedValue;
    private final String actualValue;
    private final String message;

    /**
     * Private constructor used by the builder.
     *
     * @param builder the builder containing the field values
     */
    private ConditionFailure(Builder builder) {
        this.conditionType = builder.conditionType;
        this.fieldName = builder.fieldName;
        this.operation = builder.operation;
        this.expectedValue = builder.expectedValue;
        this.actualValue = builder.actualValue;
        this.message = builder.message;
    }

    /**
     * Creates a new builder instance for constructing ConditionFailure objects.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the type of condition that failed (e.g., "Header", "QueryParam", "PathVariable").
     *
     * @return the condition type, may be null
     */
    public String getConditionType() {
        return conditionType;
    }

    /**
     * Gets the name of the field that was being validated.
     *
     * @return the field name, may be null
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets the operation that was performed (e.g., "equals", "contains", "matches").
     *
     * @return the operation, may be null
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Gets the expected value for the validation.
     *
     * @return the expected value, may be null
     */
    public String getExpectedValue() {
        return expectedValue;
    }

    /**
     * Gets the actual value that was found during validation.
     *
     * @return the actual value, may be null
     */
    public String getActualValue() {
        return actualValue;
    }

    /**
     * Gets the descriptive message explaining the failure.
     *
     * @return the failure message, may be null
     */
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConditionFailure that = (ConditionFailure) o;
        return Objects.equals(conditionType, that.conditionType) &&
               Objects.equals(fieldName, that.fieldName) &&
               Objects.equals(operation, that.operation) &&
               Objects.equals(expectedValue, that.expectedValue) &&
               Objects.equals(actualValue, that.actualValue) &&
               Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionType, fieldName, operation, expectedValue, actualValue, message);
    }

    @Override
    public String toString() {
        return "ConditionFailure{" +
               "conditionType='" + conditionType + '\'' +
               ", fieldName='" + fieldName + '\'' +
               ", operation='" + operation + '\'' +
               ", expectedValue='" + expectedValue + '\'' +
               ", actualValue='" + actualValue + '\'' +
               ", message='" + message + '\'' +
               '}';
    }

    /**
     * Builder for constructing ConditionFailure instances.
     * Provides a fluent API for setting optional fields.
     */
    public static class Builder {
        private String conditionType;
        private String fieldName;
        private String operation;
        private String expectedValue;
        private String actualValue;
        private String message;

        private Builder() {
        }

        /**
         * Sets the condition type.
         *
         * @param conditionType the type of condition (e.g., "Header", "QueryParam")
         * @return this builder instance for method chaining
         */
        public Builder conditionType(String conditionType) {
            this.conditionType = conditionType;
            return this;
        }

        /**
         * Sets the field name being validated.
         *
         * @param fieldName the name of the field
         * @return this builder instance for method chaining
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Sets the operation being performed.
         *
         * @param operation the operation (e.g., "equals", "contains")
         * @return this builder instance for method chaining
         */
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        /**
         * Sets the expected value.
         *
         * @param expectedValue the expected value for validation
         * @return this builder instance for method chaining
         */
        public Builder expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        /**
         * Sets the actual value found.
         *
         * @param actualValue the actual value during validation
         * @return this builder instance for method chaining
         */
        public Builder actualValue(String actualValue) {
            this.actualValue = actualValue;
            return this;
        }

        /**
         * Sets the descriptive failure message.
         *
         * @param message the failure message
         * @return this builder instance for method chaining
         */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Builds and returns a new ConditionFailure instance.
         *
         * @return a new immutable ConditionFailure instance
         */
        public ConditionFailure build() {
            return new ConditionFailure(this);
        }
    }
}
