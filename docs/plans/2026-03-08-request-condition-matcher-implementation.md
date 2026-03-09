# Request Condition Matcher Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a flexible HTTP request condition matching utility for routing/filtering based on headers, query parameters, and JSON payloads with both AOP and programmatic interfaces.

**Architecture:** Framework-agnostic core with Spring integration layers. Core condition evaluation engine operates on abstracted request context, supporting AND/OR boolean logic with nested groups. Spring aspect intercepts annotated methods, programmatic API provides direct service injection.

**Tech Stack:** Java 17, Spring Boot 4.0.3, Spring AOP, JUnit 5, Jayway JSONPath (optional dependency), Maven

---

## Phase 1: Core Foundation

### Task 1.1: Core Enums and Value Objects

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/EvaluationMode.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/MatchOperation.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/GroupOperator.java`

**Step 1: Write test for EvaluationMode enum**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/EvaluationModeTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationModeTest {

    @Test
    void shouldHaveFailFastMode() {
        assertNotNull(EvaluationMode.FAIL_FAST);
    }

    @Test
    void shouldHaveCollectAllMode() {
        assertNotNull(EvaluationMode.COLLECT_ALL);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=EvaluationModeTest`
Expected: Compilation error - EvaluationMode class not found

**Step 3: Implement EvaluationMode enum**

```java
package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Evaluation mode for condition matching.
 * Determines whether to stop at first failure or collect all failures.
 */
public enum EvaluationMode {
    /**
     * Stop evaluation at the first failed condition (better performance).
     */
    FAIL_FAST,

    /**
     * Evaluate all conditions and collect all failures (better debugging).
     */
    COLLECT_ALL
}
```

**Step 4: Implement MatchOperation enum**

```java
package com.cleveloper.jufu.requestutils.condition.core;

/**
 * String matching operations supported by conditions.
 */
public enum MatchOperation {
    EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    REGEX
}
```

**Step 5: Implement GroupOperator enum**

```java
package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Logical operators for combining conditions in groups.
 */
public enum GroupOperator {
    AND,
    OR
}
```

**Step 6: Run tests to verify they pass**

Run: `cd request-utils && ./mvnw test -Dtest=EvaluationModeTest`
Expected: PASS (2 tests)

**Step 7: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/*.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/EvaluationModeTest.java
git commit -m "feat(condition): add core enums for evaluation mode, match operations, and group operators"
```

---

### Task 1.2: ConditionFailure Value Object

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionFailure.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionFailureTest.java`

**Step 1: Write test for ConditionFailure**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionFailureTest`
Expected: Compilation error - ConditionFailure class not found

**Step 3: Implement ConditionFailure**

```java
package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Represents details of a failed condition.
 * Immutable value object containing all information needed for debugging.
 */
public class ConditionFailure {
    private final String conditionType;
    private final String fieldName;
    private final String operation;
    private final String expectedValue;
    private final String actualValue;
    private final String message;

    private ConditionFailure(Builder builder) {
        this.conditionType = builder.conditionType;
        this.fieldName = builder.fieldName;
        this.operation = builder.operation;
        this.expectedValue = builder.expectedValue;
        this.actualValue = builder.actualValue;
        this.message = builder.message;
    }

    public String getConditionType() { return conditionType; }
    public String getFieldName() { return fieldName; }
    public String getOperation() { return operation; }
    public String getExpectedValue() { return expectedValue; }
    public String getActualValue() { return actualValue; }
    public String getMessage() { return message; }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", conditionType, message);
    }

    public static class Builder {
        private String conditionType;
        private String fieldName;
        private String operation;
        private String expectedValue;
        private String actualValue;
        private String message;

        public Builder conditionType(String conditionType) {
            this.conditionType = conditionType;
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public Builder actualValue(String actualValue) {
            this.actualValue = actualValue;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public ConditionFailure build() {
            return new ConditionFailure(this);
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionFailureTest`
Expected: PASS (2 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionFailure.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionFailureTest.java
git commit -m "feat(condition): add ConditionFailure value object with builder"
```

---

### Task 1.3: ConditionResult Value Object

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionResult.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionResultTest.java`

**Step 1: Write test for ConditionResult**

```java
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
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionResultTest`
Expected: Compilation error - ConditionResult class not found

**Step 3: Implement ConditionResult**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionResultTest`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionResult.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionResultTest.java
git commit -m "feat(condition): add ConditionResult value object"
```

---

### Task 1.4: RequestContext Abstraction

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestContext.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextImpl.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextTest.java`

**Step 1: Write test for RequestContext**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @Test
    void shouldExtractHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "test-key");
        request.addHeader("Accept", "application/json");

        RequestContext context = RequestContext.from(request);

        assertEquals("test-key", context.getHeader("X-Api-Key"));
        assertEquals("application/json", context.getHeader("Accept"));
        assertNull(context.getHeader("NonExistent"));
    }

    @Test
    void shouldExtractQueryParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");
        request.addParameter("type", "premium");

        RequestContext context = RequestContext.from(request);

        assertEquals("123", context.getQueryParam("userId"));
        assertEquals("premium", context.getQueryParam("type"));
        assertNull(context.getQueryParam("nonExistent"));
    }

    @Test
    void shouldHandleMultiValueHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom", "value1");
        request.addHeader("X-Custom", "value2");

        RequestContext context = RequestContext.from(request);
        List<String> values = context.getHeaders("X-Custom");

        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    void shouldReturnNullForMissingJsonWhenNoBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestContext context = RequestContext.from(request);

        assertNull(context.getJsonBody());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=RequestContextTest`
Expected: Compilation error - RequestContext class not found

**Step 3: Implement RequestContext interface**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Framework-agnostic abstraction over HTTP request.
 * Provides access to headers, query parameters, and body for condition evaluation.
 */
public interface RequestContext {

    /**
     * Get the first value of a header, or null if not present.
     */
    String getHeader(String name);

    /**
     * Get all values of a header.
     */
    List<String> getHeaders(String name);

    /**
     * Get the first value of a query parameter, or null if not present.
     */
    String getQueryParam(String name);

    /**
     * Get all values of a query parameter.
     */
    List<String> getQueryParams(String name);

    /**
     * Get the raw request body as a string, or null if no body.
     */
    String getBody();

    /**
     * Get the parsed JSON body, or null if body is not JSON.
     * Lazy-parsed and cached.
     */
    Object getJsonBody();

    /**
     * Create a RequestContext from a Spring HttpServletRequest.
     */
    static RequestContext from(HttpServletRequest request) {
        return new RequestContextImpl(request);
    }
}
```

**Step 4: Implement RequestContextImpl**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of RequestContext backed by Spring's HttpServletRequest.
 */
class RequestContextImpl implements RequestContext {
    private final HttpServletRequest request;
    private String body;
    private Object jsonBody;
    private boolean jsonParsed = false;

    RequestContextImpl(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return Collections.list(request.getHeaders(name));
    }

    @Override
    public String getQueryParam(String name) {
        return request.getParameter(name);
    }

    @Override
    public List<String> getQueryParams(String name) {
        String[] values = request.getParameterValues(name);
        return values != null ? List.of(values) : Collections.emptyList();
    }

    @Override
    public String getBody() {
        if (body == null) {
            body = readBody();
        }
        return body;
    }

    @Override
    public Object getJsonBody() {
        if (!jsonParsed) {
            // JSON parsing will be implemented when JSONPath dependency is added
            // For now, return null
            jsonParsed = true;
        }
        return jsonBody;
    }

    private String readBody() {
        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
```

**Step 5: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=RequestContextTest`
Expected: PASS (4 tests)

**Step 6: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestContext*.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextTest.java
git commit -m "feat(condition): add RequestContext abstraction for framework-agnostic request access"
```

---

### Task 1.5: Condition Interface

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/Condition.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionTest.java`

**Step 1: Write test with dummy implementation**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ConditionTest {

    @Test
    void shouldEvaluateCondition() {
        Condition condition = new TestCondition(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldReturnFailureWhenConditionFails() {
        Condition condition = new TestCondition(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
    }

    // Test implementation
    private static class TestCondition implements Condition {
        private final boolean shouldMatch;

        TestCondition(boolean shouldMatch) {
            this.shouldMatch = shouldMatch;
        }

        @Override
        public ConditionResult evaluate(RequestContext context) {
            return shouldMatch ? ConditionResult.success()
                              : ConditionResult.failure(
                                  ConditionFailure.builder()
                                      .message("Test condition failed")
                                      .build()
                              );
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionTest`
Expected: Compilation error - Condition interface not found

**Step 3: Implement Condition interface**

```java
package com.cleveloper.jufu.requestutils.condition.core;

/**
 * Core interface for all condition types.
 * Conditions evaluate a request context and return a result indicating success or failure.
 */
@FunctionalInterface
public interface Condition {

    /**
     * Evaluate this condition against the given request context.
     *
     * @param context the request context to evaluate
     * @return result indicating whether the condition matched
     */
    ConditionResult evaluate(RequestContext context);
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionTest`
Expected: PASS (2 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/Condition.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionTest.java
git commit -m "feat(condition): add core Condition interface"
```

---

## Phase 2: Condition Matchers

### Task 2.1: String Matcher Utility

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/StringMatcher.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/StringMatcherTest.java`

**Step 1: Write test for StringMatcher**

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.MatchOperation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringMatcherTest {

    @Test
    void shouldMatchEquals() {
        assertTrue(StringMatcher.matches("test", "test", MatchOperation.EQUALS, false));
        assertFalse(StringMatcher.matches("test", "other", MatchOperation.EQUALS, false));
    }

    @Test
    void shouldMatchEqualsCaseInsensitive() {
        assertTrue(StringMatcher.matches("TEST", "test", MatchOperation.EQUALS, true));
        assertTrue(StringMatcher.matches("test", "TEST", MatchOperation.EQUALS, true));
    }

    @Test
    void shouldMatchContains() {
        assertTrue(StringMatcher.matches("hello world", "world", MatchOperation.CONTAINS, false));
        assertFalse(StringMatcher.matches("hello world", "foo", MatchOperation.CONTAINS, false));
    }

    @Test
    void shouldMatchStartsWith() {
        assertTrue(StringMatcher.matches("hello world", "hello", MatchOperation.STARTS_WITH, false));
        assertFalse(StringMatcher.matches("hello world", "world", MatchOperation.STARTS_WITH, false));
    }

    @Test
    void shouldMatchEndsWith() {
        assertTrue(StringMatcher.matches("hello world", "world", MatchOperation.ENDS_WITH, false));
        assertFalse(StringMatcher.matches("hello world", "hello", MatchOperation.ENDS_WITH, false));
    }

    @Test
    void shouldMatchRegex() {
        assertTrue(StringMatcher.matches("test123", "test\\d+", MatchOperation.REGEX, false));
        assertFalse(StringMatcher.matches("testabc", "test\\d+", MatchOperation.REGEX, false));
    }

    @Test
    void shouldHandleNullActualValue() {
        assertFalse(StringMatcher.matches(null, "test", MatchOperation.EQUALS, false));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=StringMatcherTest`
Expected: Compilation error - StringMatcher class not found

**Step 3: Implement StringMatcher**

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.MatchOperation;
import java.util.regex.Pattern;

/**
 * Utility for matching strings based on various operations.
 */
public class StringMatcher {

    /**
     * Check if the actual value matches the expected value using the specified operation.
     *
     * @param actualValue the actual value from the request
     * @param expectedValue the expected value to match against
     * @param operation the matching operation to perform
     * @param ignoreCase whether to perform case-insensitive matching
     * @return true if the values match according to the operation
     */
    public static boolean matches(String actualValue, String expectedValue,
                                  MatchOperation operation, boolean ignoreCase) {
        if (actualValue == null) {
            return false;
        }

        String actual = ignoreCase ? actualValue.toLowerCase() : actualValue;
        String expected = ignoreCase ? expectedValue.toLowerCase() : expectedValue;

        return switch (operation) {
            case EQUALS -> actual.equals(expected);
            case CONTAINS -> actual.contains(expected);
            case STARTS_WITH -> actual.startsWith(expected);
            case ENDS_WITH -> actual.endsWith(expected);
            case REGEX -> matchesRegex(actualValue, expectedValue, ignoreCase);
        };
    }

    private static boolean matchesRegex(String actualValue, String pattern, boolean ignoreCase) {
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        return Pattern.compile(pattern, flags).matcher(actualValue).matches();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=StringMatcherTest`
Expected: PASS (7 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/StringMatcher.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/StringMatcherTest.java
git commit -m "feat(condition): add StringMatcher utility for string operations"
```

---

### Task 2.2: HeaderCondition

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/HeaderCondition.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/HeaderConditionTest.java`

**Step 1: Write test for HeaderCondition**

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class HeaderConditionTest {

    @Test
    void shouldMatchWhenHeaderEquals() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "premium", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldFailWhenHeaderDoesNotMatch() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "premium", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "basic");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());

        ConditionFailure failure = result.getFailures().get(0);
        assertEquals("Header", failure.getConditionType());
        assertEquals("X-Api-Key", failure.getFieldName());
        assertTrue(failure.getMessage().contains("premium"));
        assertTrue(failure.getMessage().contains("basic"));
    }

    @Test
    void shouldFailWhenHeaderMissing() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "premium", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertTrue(result.getFailures().get(0).getMessage().contains("missing"));
    }

    @Test
    void shouldMatchCaseInsensitive() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "PREMIUM", MatchOperation.EQUALS, true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=HeaderConditionTest`
Expected: Compilation error - HeaderCondition class not found

**Step 3: Implement HeaderCondition**

```java
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
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=HeaderConditionTest`
Expected: PASS (4 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/HeaderCondition.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/HeaderConditionTest.java
git commit -m "feat(condition): add HeaderCondition for matching HTTP headers"
```

---

### Task 2.3: QueryParamCondition

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/QueryParamCondition.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/QueryParamConditionTest.java`

**Step 1: Write test for QueryParamCondition**

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class QueryParamConditionTest {

    @Test
    void shouldMatchWhenQueryParamEquals() {
        QueryParamCondition condition = new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldFailWhenQueryParamDoesNotMatch() {
        QueryParamCondition condition = new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "456");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals("QueryParam", result.getFailures().get(0).getConditionType());
    }

    @Test
    void shouldFailWhenQueryParamMissing() {
        QueryParamCondition condition = new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertTrue(result.getFailures().get(0).getMessage().contains("missing"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=QueryParamConditionTest`
Expected: Compilation error - QueryParamCondition class not found

**Step 3: Implement QueryParamCondition**

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;

/**
 * Condition that matches query parameter values.
 */
public class QueryParamCondition implements Condition {
    private final String paramName;
    private final String expectedValue;
    private final MatchOperation operation;
    private final boolean ignoreCase;

    public QueryParamCondition(String paramName, String expectedValue,
                              MatchOperation operation, boolean ignoreCase) {
        this.paramName = paramName;
        this.expectedValue = expectedValue;
        this.operation = operation;
        this.ignoreCase = ignoreCase;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        String actualValue = context.getQueryParam(paramName);

        if (actualValue == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("QueryParam")
                    .fieldName(paramName)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(null)
                    .message(String.format("Query parameter '%s' is missing", paramName))
                    .build()
            );
        }

        boolean matches = StringMatcher.matches(actualValue, expectedValue, operation, ignoreCase);

        if (matches) {
            return ConditionResult.success();
        } else {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("QueryParam")
                    .fieldName(paramName)
                    .operation(operation.name())
                    .expectedValue(expectedValue)
                    .actualValue(actualValue)
                    .message(String.format("Query parameter '%s' expected to %s '%s' but was '%s'",
                        paramName, operation.name().toLowerCase().replace('_', ' '),
                        expectedValue, actualValue))
                    .build()
            );
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=QueryParamConditionTest`
Expected: PASS (3 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/QueryParamCondition.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/QueryParamConditionTest.java
git commit -m "feat(condition): add QueryParamCondition for matching query parameters"
```

---

## Phase 3: Condition Groups and Matcher Service

### Task 3.1: ConditionGroup

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionGroup.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionGroupTest.java`

**Step 1: Write test for ConditionGroup**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ConditionGroupTest {

    @Test
    void shouldEvaluateAndGroupWithAllPassing() {
        Condition condition1 = ctx -> ConditionResult.success();
        Condition condition2 = ctx -> ConditionResult.success();

        ConditionGroup group = ConditionGroup.and(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateAndGroupWithOneFailing() {
        Condition condition1 = ctx -> ConditionResult.success();
        Condition condition2 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed").build()
        );

        ConditionGroup group = ConditionGroup.and(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());
    }

    @Test
    void shouldEvaluateOrGroupWithOnePassing() {
        Condition condition1 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed").build()
        );
        Condition condition2 = ctx -> ConditionResult.success();

        ConditionGroup group = ConditionGroup.or(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateOrGroupWithAllFailing() {
        Condition condition1 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed 1").build()
        );
        Condition condition2 = ctx -> ConditionResult.failure(
            ConditionFailure.builder().message("Failed 2").build()
        );

        ConditionGroup group = ConditionGroup.or(condition1, condition2);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertFalse(result.isMatched());
        assertEquals(2, result.getFailures().size());
    }

    @Test
    void shouldSupportNestedGroups() {
        Condition c1 = ctx -> ConditionResult.success();
        Condition c2 = ctx -> ConditionResult.success();
        Condition c3 = ctx -> ConditionResult.success();

        // (c1 OR c2) AND c3
        ConditionGroup orGroup = ConditionGroup.or(c1, c2);
        ConditionGroup andGroup = ConditionGroup.and(orGroup, c3);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = andGroup.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionGroupTest`
Expected: Compilation error - ConditionGroup class not found

**Step 3: Implement ConditionGroup**

```java
package com.cleveloper.jufu.requestutils.condition.core;

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
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionGroupTest`
Expected: PASS (5 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionGroup.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/ConditionGroupTest.java
git commit -m "feat(condition): add ConditionGroup for AND/OR logic with nested groups"
```

---

### Task 3.2: ConditionGroup Builder

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/builder/ConditionGroupBuilder.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/builder/ConditionGroupBuilderTest.java`

**Step 1: Write test for ConditionGroupBuilder**

```java
package com.cleveloper.jufu.requestutils.condition.builder;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ConditionGroupBuilderTest {

    @Test
    void shouldBuildSimpleAndGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(ctx -> ConditionResult.success())
            .and(ctx -> ConditionResult.success())
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldBuildSimpleOrGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .or(ctx -> ConditionResult.success())
            .or(ctx -> ConditionResult.failure(
                ConditionFailure.builder().message("Failed").build()
            ))
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldBuildNestedAndGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(ctx -> ConditionResult.success())
            .andGroup(builder -> builder
                .and(ctx -> ConditionResult.success())
                .and(ctx -> ConditionResult.success())
            )
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldBuildNestedOrGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(ctx -> ConditionResult.success())
            .orGroup(builder -> builder
                .or(ctx -> ConditionResult.success())
                .or(ctx -> ConditionResult.failure(
                    ConditionFailure.builder().message("Failed").build()
                ))
            )
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        ConditionResult result = group.evaluate(RequestContext.from(request));

        assertTrue(result.isMatched());
    }

    @Test
    void shouldSetEvaluationMode() {
        ConditionGroup group = ConditionGroup.builder()
            .mode(EvaluationMode.COLLECT_ALL)
            .and(ctx -> ConditionResult.success())
            .build();

        assertNotNull(group);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionGroupBuilderTest`
Expected: Compilation error - builder() method not found

**Step 3: Add builder method to ConditionGroup**

Modify: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionGroup.java`

Add to the end of the class:

```java
    /**
     * Create a new builder for fluent construction of condition groups.
     */
    public static ConditionGroupBuilder builder() {
        return new ConditionGroupBuilder();
    }
```

**Step 4: Implement ConditionGroupBuilder**

```java
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
```

**Step 5: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionGroupBuilderTest`
Expected: PASS (5 tests)

**Step 6: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/ConditionGroup.java request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/builder/ConditionGroupBuilder.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/builder/ConditionGroupBuilderTest.java
git commit -m "feat(condition): add fluent ConditionGroupBuilder with nested group support"
```

---

### Task 3.3: RequestConditionMatcher Service

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestConditionMatcher.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestConditionMatcherTest.java`

**Step 1: Write test for RequestConditionMatcher**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import com.cleveloper.jufu.requestutils.condition.matchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class RequestConditionMatcherTest {

    private final RequestConditionMatcher matcher = new RequestConditionMatcher();

    @Test
    void shouldEvaluateSingleCondition() {
        Condition condition = new HeaderCondition("X-Api-Key", "test", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "test");

        ConditionResult result = matcher.evaluate(condition, request);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateConditionGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(new HeaderCondition("X-Api-Key", "test", MatchOperation.EQUALS, false))
            .and(new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false))
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "test");
        request.addParameter("userId", "123");

        ConditionResult result = matcher.evaluate(group, request);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldReturnFailuresWhenConditionFails() {
        Condition condition = new HeaderCondition("X-Api-Key", "expected", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "actual");

        ConditionResult result = matcher.evaluate(condition, request);

        assertFalse(result.isMatched());
        assertFalse(result.getFailures().isEmpty());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=RequestConditionMatcherTest`
Expected: Compilation error - RequestConditionMatcher class not found

**Step 3: Implement RequestConditionMatcher**

```java
package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Main service for evaluating conditions against HTTP requests.
 * Framework-agnostic - operates on the Condition interface and RequestContext abstraction.
 */
public class RequestConditionMatcher {

    /**
     * Evaluate a condition against an HTTP request.
     *
     * @param condition the condition to evaluate
     * @param request the HTTP request
     * @return the evaluation result
     */
    public ConditionResult evaluate(Condition condition, HttpServletRequest request) {
        RequestContext context = RequestContext.from(request);
        return condition.evaluate(context);
    }

    /**
     * Evaluate a condition against a request context.
     *
     * @param condition the condition to evaluate
     * @param context the request context
     * @return the evaluation result
     */
    public ConditionResult evaluate(Condition condition, RequestContext context) {
        return condition.evaluate(context);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=RequestConditionMatcherTest`
Expected: PASS (3 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestConditionMatcher.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestConditionMatcherTest.java
git commit -m "feat(condition): add RequestConditionMatcher service for condition evaluation"
```

---

## Phase 4: Exception and Spring Configuration

### Task 4.1: ConditionNotMetException

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionNotMetException.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionNotMetExceptionTest.java`

**Step 1: Write test for ConditionNotMetException**

```java
package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConditionNotMetExceptionTest {

    @Test
    void shouldCreateExceptionWithResult() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .message("Test failure")
            .build();
        ConditionResult result = ConditionResult.failure(failure);

        ConditionNotMetException exception = new ConditionNotMetException(result);

        assertEquals(result, exception.getResult());
        assertTrue(exception.getMessage().contains("Test failure"));
    }

    @Test
    void shouldFormatMessageWithMultipleFailures() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .conditionType("Header")
            .message("Failure 1")
            .build();
        ConditionFailure failure2 = ConditionFailure.builder()
            .conditionType("QueryParam")
            .message("Failure 2")
            .build();
        ConditionResult result = ConditionResult.failure(List.of(failure1, failure2));

        ConditionNotMetException exception = new ConditionNotMetException(result);

        String message = exception.getMessage();
        assertTrue(message.contains("Failure 1"));
        assertTrue(message.contains("Failure 2"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionNotMetExceptionTest`
Expected: Compilation error - ConditionNotMetException class not found

**Step 3: Implement ConditionNotMetException**

```java
package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.core.ConditionResult;
import java.util.stream.Collectors;

/**
 * Exception thrown when request conditions are not met.
 * Contains detailed information about which conditions failed.
 */
public class ConditionNotMetException extends RuntimeException {
    private final ConditionResult result;

    public ConditionNotMetException(ConditionResult result) {
        super(formatMessage(result));
        this.result = result;
    }

    public ConditionResult getResult() {
        return result;
    }

    private static String formatMessage(ConditionResult result) {
        if (result.getFailures().isEmpty()) {
            return "Request conditions not met";
        }

        if (result.getFailures().size() == 1) {
            return "Request condition not met: " + result.getFailures().get(0).getMessage();
        }

        return "Request conditions not met:\n" +
            result.getFailures().stream()
                .map(f -> "  - " + f.toString())
                .collect(Collectors.joining("\n"));
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionNotMetExceptionTest`
Expected: PASS (2 tests)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionNotMetException.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionNotMetExceptionTest.java
git commit -m "feat(condition): add ConditionNotMetException with formatted failure messages"
```

---

### Task 4.2: Spring Boot Auto-Configuration

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfiguration.java`
- Create: `request-utils/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**Step 1: Implement ConditionMatcherAutoConfiguration**

```java
package com.cleveloper.jufu.requestutils.condition.config;

import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for request condition matching.
 * Registers the RequestConditionMatcher as a Spring bean.
 */
@AutoConfiguration
public class ConditionMatcherAutoConfiguration {

    @Bean
    public RequestConditionMatcher requestConditionMatcher() {
        return new RequestConditionMatcher();
    }
}
```

**Step 2: Create auto-configuration imports file**

Create directory and file:

```bash
mkdir -p request-utils/src/main/resources/META-INF/spring
```

Content for `request-utils/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.cleveloper.jufu.requestutils.condition.config.ConditionMatcherAutoConfiguration
```

**Step 3: Write integration test**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfigurationTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.config;

import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConditionMatcherAutoConfigurationTest {

    @Autowired(required = false)
    private RequestConditionMatcher matcher;

    @Test
    void shouldAutoConfigureRequestConditionMatcher() {
        assertNotNull(matcher);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionMatcherAutoConfigurationTest`
Expected: PASS (1 test)

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfiguration.java request-utils/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfigurationTest.java
git commit -m "feat(condition): add Spring Boot auto-configuration for RequestConditionMatcher"
```

---

## Phase 5: Annotations

### Task 5.1: Core Annotations - EvaluationMode and Inline Matchers

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUMatchConditions.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUCondition.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUHeader.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUQueryParam.java`

**Step 1: Implement JUFUHeader annotation**

```java
package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline header matching configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUHeader {

    /**
     * Header name to match.
     */
    String name();

    /**
     * Exact value to match (mutually exclusive with other operations).
     */
    String equals() default "";

    /**
     * Substring to match (mutually exclusive with other operations).
     */
    String contains() default "";

    /**
     * Prefix to match (mutually exclusive with other operations).
     */
    String startsWith() default "";

    /**
     * Suffix to match (mutually exclusive with other operations).
     */
    String endsWith() default "";

    /**
     * Regex pattern to match (mutually exclusive with other operations).
     */
    String regex() default "";

    /**
     * Whether to ignore case during matching.
     */
    boolean ignoreCase() default false;
}
```

**Step 2: Implement JUFUQueryParam annotation**

```java
package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline query parameter matching configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUQueryParam {

    /**
     * Query parameter name to match.
     */
    String name();

    /**
     * Exact value to match (mutually exclusive with other operations).
     */
    String equals() default "";

    /**
     * Substring to match (mutually exclusive with other operations).
     */
    String contains() default "";

    /**
     * Prefix to match (mutually exclusive with other operations).
     */
    String startsWith() default "";

    /**
     * Suffix to match (mutually exclusive with other operations).
     */
    String endsWith() default "";

    /**
     * Regex pattern to match (mutually exclusive with other operations).
     */
    String regex() default "";

    /**
     * Whether to ignore case during matching.
     */
    boolean ignoreCase() default false;
}
```

**Step 3: Implement JUFUCondition annotation**

```java
package com.cleveloper.jufu.requestutils.condition.annotations;

import com.cleveloper.jufu.requestutils.condition.core.Condition;
import java.lang.annotation.*;

/**
 * Individual condition definition.
 * Supports class reference mode, inline mode, or hybrid.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Repeatable(JUFUMatchConditions.class)
public @interface JUFUCondition {

    /**
     * Condition class to instantiate (class reference mode).
     */
    Class<? extends Condition> value() default Condition.class;

    /**
     * Inline header condition.
     */
    JUFUHeader header() default @JUFUHeader(name = "");

    /**
     * Inline query parameter condition.
     */
    JUFUQueryParam queryParam() default @JUFUQueryParam(name = "");
}
```

**Step 4: Implement JUFUMatchConditions annotation**

```java
package com.cleveloper.jufu.requestutils.condition.annotations;

import com.cleveloper.jufu.requestutils.condition.core.EvaluationMode;
import java.lang.annotation.*;

/**
 * Container annotation for multiple conditions.
 * Can be placed on methods or classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface JUFUMatchConditions {

    /**
     * Array of conditions to evaluate.
     */
    JUFUCondition[] value();

    /**
     * Evaluation mode for these conditions.
     */
    EvaluationMode mode() default EvaluationMode.FAIL_FAST;
}
```

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/*.java
git commit -m "feat(condition): add JUFU-prefixed annotations for declarative condition matching"
```

---

## Summary

This implementation plan provides a complete, test-driven approach to building the request condition matcher utility. The plan is organized into 5 phases:

1. **Phase 1: Core Foundation** - Basic value objects and abstractions (5 tasks)
2. **Phase 2: Condition Matchers** - String matching and specific condition types (3 tasks)
3. **Phase 3: Groups and Matcher** - Condition groups with AND/OR logic and main service (3 tasks)
4. **Phase 4: Exception and Config** - Spring integration and auto-configuration (2 tasks)
5. **Phase 5: Annotations** - Declarative annotation layer (1 task)

Each task follows TDD principles: write test → verify failure → implement → verify pass → commit.

**Note:** JSON matching (JSONPath and exact match), AOP aspect implementation, and annotation processing will be covered in a follow-up implementation plan to keep this plan focused and manageable.

**Total estimated tasks:** 14 core tasks (approximately 2-5 minutes each)
**Total estimated time:** 30-70 minutes for core implementation
