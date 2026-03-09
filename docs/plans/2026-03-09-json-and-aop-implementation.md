# JSON Matching and AOP Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add JSON payload matching and annotation-based AOP integration to the request condition matcher.

**Architecture:** Extend the existing condition matching engine with JSON support via JSONPath library (optional dependency). Add Spring AOP layer to intercept annotated methods and evaluate conditions declaratively. Use @ConditionalOnClass to ensure JSON features only activate when dependencies are present.

**Tech Stack:** Java 17, Spring Boot 4.0.3, Spring AOP, Jayway JSONPath 2.9.0, Jackson (included with Spring Boot), JUnit 5, Maven

---

## Phase 1: JSON Matching Support

### Task 1.1: Add JSONPath Dependency

**Files:**
- Modify: `request-utils/pom.xml`

**Step 1: Add JSONPath dependency to pom.xml**

Add after the existing dependencies (around line 40):

```xml
<!-- Optional: JSON path matching support -->
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.9.0</version>
    <optional>true</optional>
</dependency>
```

**Step 2: Build to verify dependency downloads**

Run: `cd request-utils && ./mvnw clean compile`
Expected: BUILD SUCCESS with JSONPath dependency downloaded

**Step 3: Commit**

```bash
git add request-utils/pom.xml
git commit -m "build: add JSONPath optional dependency for JSON matching"
```

---

### Task 1.2: Update RequestContextImpl for JSON Parsing

**Files:**
- Modify: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextImpl.java:52-59`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextImplTest.java`

**Step 1: Write test for JSON body parsing**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextImplTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RequestContextImplTest {

    @Test
    void shouldParseJsonBody() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        String jsonString = "{\"user\":\"john\",\"age\":30}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(jsonString)));
        when(request.getContentType()).thenReturn("application/json");

        RequestContext context = RequestContext.from(request);
        Object jsonBody = context.getJsonBody();

        assertNotNull(jsonBody);
        assertTrue(jsonBody instanceof JsonNode);
        JsonNode node = (JsonNode) jsonBody;
        assertEquals("john", node.get("user").asText());
        assertEquals(30, node.get("age").asInt());
    }

    @Test
    void shouldReturnNullForNonJsonContent() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        String plainText = "not json";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(plainText)));
        when(request.getContentType()).thenReturn("text/plain");

        RequestContext context = RequestContext.from(request);
        Object jsonBody = context.getJsonBody();

        assertNull(jsonBody);
    }

    @Test
    void shouldCacheJsonBody() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        String jsonString = "{\"cached\":true}";
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(jsonString)));
        when(request.getContentType()).thenReturn("application/json");

        RequestContext context = RequestContext.from(request);
        Object first = context.getJsonBody();
        Object second = context.getJsonBody();

        assertSame(first, second); // Same instance (cached)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=RequestContextImplTest`
Expected: FAIL - JSON parsing not implemented

**Step 3: Update RequestContextImpl to parse JSON**

Modify `RequestContextImpl.java`:

1. Add Jackson import at top:
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
```

2. Add ObjectMapper field after existing fields (line 16):
```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
```

3. Replace getJsonBody() method (lines 52-59):
```java
@Override
public Object getJsonBody() {
    if (!jsonParsed) {
        jsonParsed = true;
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            String bodyContent = getBody();
            if (bodyContent != null && !bodyContent.isEmpty()) {
                try {
                    jsonBody = OBJECT_MAPPER.readTree(bodyContent);
                } catch (IOException e) {
                    // Invalid JSON - return null
                    jsonBody = null;
                }
            }
        }
    }
    return jsonBody;
}
```

4. Update constructor to accept HttpServletRequest (line 18):
```java
RequestContextImpl(HttpServletRequest request) {
    this.request = request;
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=RequestContextImplTest`
Expected: PASS - All 3 tests passing

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextImpl.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/core/RequestContextImplTest.java
git commit -m "feat(condition): add JSON body parsing to RequestContext"
```

---

### Task 1.3: JsonPathCondition Implementation

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonPathCondition.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonPathConditionTest.java`

**Step 1: Write test for JsonPathCondition**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonPathConditionTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JsonPathConditionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldMatchJsonPath() throws Exception {
        RequestContext context = Mockito.mock(RequestContext.class);
        String json = "{\"user\":{\"email\":\"john@example.com\"}}";
        when(context.getJsonBody()).thenReturn(MAPPER.readTree(json));

        JsonPathCondition condition = new JsonPathCondition(
            "$.user.email",
            "@example.com",
            MatchOperation.CONTAINS,
            false
        );

        ConditionResult result = condition.evaluate(context);
        assertTrue(result.isMatched());
    }

    @Test
    void shouldFailWhenPathNotFound() throws Exception {
        RequestContext context = Mockito.mock(RequestContext.class);
        String json = "{\"user\":{\"name\":\"john\"}}";
        when(context.getJsonBody()).thenReturn(MAPPER.readTree(json));

        JsonPathCondition condition = new JsonPathCondition(
            "$.user.email",
            "@example.com",
            MatchOperation.CONTAINS,
            false
        );

        ConditionResult result = condition.evaluate(context);
        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());
        assertTrue(result.getFailures().get(0).getMessage().contains("not found"));
    }

    @Test
    void shouldFailWhenJsonBodyNull() {
        RequestContext context = Mockito.mock(RequestContext.class);
        when(context.getJsonBody()).thenReturn(null);

        JsonPathCondition condition = new JsonPathCondition(
            "$.user.email",
            "@example.com",
            MatchOperation.CONTAINS,
            false
        );

        ConditionResult result = condition.evaluate(context);
        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());
        assertTrue(result.getFailures().get(0).getMessage().contains("No JSON body"));
    }

    @Test
    void shouldSupportArrayAccess() throws Exception {
        RequestContext context = Mockito.mock(RequestContext.class);
        String json = "{\"items\":[{\"name\":\"item1\"},{\"name\":\"item2\"}]}";
        when(context.getJsonBody()).thenReturn(MAPPER.readTree(json));

        JsonPathCondition condition = new JsonPathCondition(
            "$.items[0].name",
            "item1",
            MatchOperation.EQUALS,
            false
        );

        ConditionResult result = condition.evaluate(context);
        assertTrue(result.isMatched());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=JsonPathConditionTest`
Expected: Compilation error - JsonPathCondition class not found

**Step 3: Implement JsonPathCondition**

Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonPathCondition.java`

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Condition that matches values extracted from JSON payload using JSONPath expressions.
 * Requires json-path library on classpath.
 */
public class JsonPathCondition implements Condition {

    private final String jsonPath;
    private final String expectedValue;
    private final MatchOperation operation;
    private final boolean ignoreCase;

    public JsonPathCondition(String jsonPath, String expectedValue, MatchOperation operation, boolean ignoreCase) {
        this.jsonPath = jsonPath;
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
                    .fieldName(jsonPath)
                    .operation(operation.name().toLowerCase())
                    .expectedValue(expectedValue)
                    .actualValue("[no JSON body]")
                    .message("No JSON body present in request")
                    .build()
            );
        }

        try {
            // Convert JsonNode to string for JSONPath processing
            String jsonString = jsonBody.toString();
            Object value = JsonPath.read(jsonString, jsonPath);
            String actualValue = value != null ? value.toString() : null;

            if (actualValue == null) {
                return failure("null");
            }

            boolean matches = StringMatcher.matches(actualValue, expectedValue, operation, ignoreCase);

            if (matches) {
                return ConditionResult.success();
            }

            return failure(actualValue);

        } catch (PathNotFoundException e) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonPath")
                    .fieldName(jsonPath)
                    .operation(operation.name().toLowerCase())
                    .expectedValue(expectedValue)
                    .actualValue("[path not found]")
                    .message("JSONPath '" + jsonPath + "' not found in request body")
                    .build()
            );
        }
    }

    private ConditionResult failure(String actualValue) {
        return ConditionResult.failure(
            ConditionFailure.builder()
                .conditionType("JsonPath")
                .fieldName(jsonPath)
                .operation(operation.name().toLowerCase())
                .expectedValue(expectedValue)
                .actualValue(actualValue)
                .message("JSONPath '" + jsonPath + "' expected to " + operation.name().toLowerCase().replace('_', ' ') +
                    " '" + expectedValue + "' but was '" + actualValue + "'")
                .build()
        );
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=JsonPathConditionTest`
Expected: PASS - All 4 tests passing

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonPathCondition.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonPathConditionTest.java
git commit -m "feat(condition): add JsonPathCondition for JSON payload matching"
```

---

### Task 1.4: JsonExactMatchCondition Implementation

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonExactMatchCondition.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonExactMatchConditionTest.java`

**Step 1: Write test for JsonExactMatchCondition**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonExactMatchConditionTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JsonExactMatchConditionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldMatchExactFields() throws Exception {
        RequestContext context = Mockito.mock(RequestContext.class);
        String requestJson = "{\"type\":\"premium\",\"region\":\"US\",\"extra\":\"ignored\"}";
        when(context.getJsonBody()).thenReturn(MAPPER.readTree(requestJson));

        String template = "{\"type\":\"premium\",\"region\":\"US\"}";
        JsonExactMatchCondition condition = new JsonExactMatchCondition(template, new String[]{"type", "region"});

        ConditionResult result = condition.evaluate(context);
        assertTrue(result.isMatched());
    }

    @Test
    void shouldFailWhenFieldDoesNotMatch() throws Exception {
        RequestContext context = Mockito.mock(RequestContext.class);
        String requestJson = "{\"type\":\"basic\",\"region\":\"US\"}";
        when(context.getJsonBody()).thenReturn(MAPPER.readTree(requestJson));

        String template = "{\"type\":\"premium\",\"region\":\"US\"}";
        JsonExactMatchCondition condition = new JsonExactMatchCondition(template, new String[]{"type", "region"});

        ConditionResult result = condition.evaluate(context);
        assertFalse(result.isMatched());
        assertTrue(result.getFailures().get(0).getMessage().contains("type"));
    }

    @Test
    void shouldFailWhenFieldMissing() throws Exception {
        RequestContext context = Mockito.mock(RequestContext.class);
        String requestJson = "{\"type\":\"premium\"}";
        when(context.getJsonBody()).thenReturn(MAPPER.readTree(requestJson));

        String template = "{\"type\":\"premium\",\"region\":\"US\"}";
        JsonExactMatchCondition condition = new JsonExactMatchCondition(template, new String[]{"type", "region"});

        ConditionResult result = condition.evaluate(context);
        assertFalse(result.isMatched());
        assertTrue(result.getFailures().get(0).getMessage().contains("missing"));
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=JsonExactMatchConditionTest`
Expected: Compilation error - JsonExactMatchCondition class not found

**Step 3: Implement JsonExactMatchCondition**

Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonExactMatchCondition.java`

```java
package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Condition that matches specific fields in JSON payload against a template.
 * Only specified fields are compared; extra fields in request are ignored.
 */
public class JsonExactMatchCondition implements Condition {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String template;
    private final String[] fields;

    public JsonExactMatchCondition(String template, String[] fields) {
        this.template = template;
        this.fields = fields;
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        Object jsonBody = context.getJsonBody();

        if (jsonBody == null) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonExactMatch")
                    .fieldName("body")
                    .operation("exact match")
                    .expectedValue("JSON body")
                    .actualValue("[no JSON body]")
                    .message("No JSON body present in request")
                    .build()
            );
        }

        try {
            JsonNode templateNode = MAPPER.readTree(template);
            JsonNode requestNode = (JsonNode) jsonBody;
            List<ConditionFailure> failures = new ArrayList<>();

            for (String field : fields) {
                JsonNode expectedValue = templateNode.get(field);
                JsonNode actualValue = requestNode.get(field);

                if (actualValue == null) {
                    failures.add(
                        ConditionFailure.builder()
                            .conditionType("JsonExactMatch")
                            .fieldName(field)
                            .operation("exact match")
                            .expectedValue(expectedValue != null ? expectedValue.toString() : "null")
                            .actualValue("[missing]")
                            .message("Field '" + field + "' is missing in request body")
                            .build()
                    );
                } else if (!actualValue.equals(expectedValue)) {
                    failures.add(
                        ConditionFailure.builder()
                            .conditionType("JsonExactMatch")
                            .fieldName(field)
                            .operation("exact match")
                            .expectedValue(expectedValue != null ? expectedValue.toString() : "null")
                            .actualValue(actualValue.toString())
                            .message("Field '" + field + "' expected to be '" + expectedValue + "' but was '" + actualValue + "'")
                            .build()
                    );
                }
            }

            if (failures.isEmpty()) {
                return ConditionResult.success();
            }

            return ConditionResult.failure(failures);

        } catch (IOException e) {
            return ConditionResult.failure(
                ConditionFailure.builder()
                    .conditionType("JsonExactMatch")
                    .fieldName("template")
                    .operation("parse")
                    .expectedValue("valid JSON")
                    .actualValue("invalid")
                    .message("Failed to parse template JSON: " + e.getMessage())
                    .build()
            );
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=JsonExactMatchConditionTest`
Expected: PASS - All 3 tests passing

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonExactMatchCondition.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/matchers/JsonExactMatchConditionTest.java
git commit -m "feat(condition): add JsonExactMatchCondition for exact field matching"
```

---

### Task 1.5: JSON Annotations

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUJsonPath.java`
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUJsonExactMatch.java`
- Modify: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUCondition.java`

**Step 1: Implement JUFUJsonPath annotation**

Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUJsonPath.java`

```java
package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline JSON path matching configuration.
 * Extracts values from JSON payload using JSONPath expressions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUJsonPath {

    /**
     * JSONPath expression to extract value (e.g., "$.user.email", "$.items[0].name").
     */
    String path();

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

**Step 2: Implement JUFUJsonExactMatch annotation**

Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUJsonExactMatch.java`

```java
package com.cleveloper.jufu.requestutils.condition.annotations;

import java.lang.annotation.*;

/**
 * Inline JSON exact field matching configuration.
 * Compares specific fields against a template.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface JUFUJsonExactMatch {

    /**
     * Template JSON string to match against.
     */
    String template();

    /**
     * Array of field names to compare.
     */
    String[] fields();
}
```

**Step 3: Update JUFUCondition to include JSON annotations**

Modify `JUFUCondition.java`, add after queryParam():

```java
/**
 * Inline JSON path condition.
 */
JUFUJsonPath jsonPath() default @JUFUJsonPath(path = "");

/**
 * Inline JSON exact match condition.
 */
JUFUJsonExactMatch jsonExactMatch() default @JUFUJsonExactMatch(template = "", fields = {});
```

**Step 4: Build to verify annotations compile**

Run: `cd request-utils && ./mvnw clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUJsonPath.java request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUJsonExactMatch.java request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/annotations/JUFUCondition.java
git commit -m "feat(condition): add JSON annotations for declarative JSON matching"
```

---

## Phase 2: AOP Integration

### Task 2.1: AnnotationConditionParser - Header and QueryParam

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/AnnotationConditionParser.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/AnnotationConditionParserTest.java`

**Step 1: Write test for parsing header annotations**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/AnnotationConditionParserTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationConditionParserTest {

    private final AnnotationConditionParser parser = new AnnotationConditionParser();

    @Test
    void shouldParseHeaderCondition() {
        JUFUHeader header = createHeaderAnnotation("X-Api-Key", "secret", "", "", "", "", false);
        JUFUCondition condition = createCondition(Condition.class, header, null, null, null);

        Condition parsed = parser.parse(condition);

        assertNotNull(parsed);
        assertTrue(parsed instanceof HeaderCondition);
    }

    @Test
    void shouldParseQueryParamCondition() {
        JUFUQueryParam queryParam = createQueryParamAnnotation("version", "", "v2", "", "", "", false);
        JUFUCondition condition = createCondition(Condition.class, null, queryParam, null, null);

        Condition parsed = parser.parse(condition);

        assertNotNull(parsed);
        assertTrue(parsed instanceof QueryParamCondition);
    }

    @Test
    void shouldParseMultipleConditionsAsGroup() {
        JUFUHeader header = createHeaderAnnotation("X-Type", "premium", "", "", "", "", false);
        JUFUQueryParam queryParam = createQueryParamAnnotation("version", "v2", "", "", "", "", false);

        JUFUCondition cond1 = createCondition(Condition.class, header, null, null, null);
        JUFUCondition cond2 = createCondition(Condition.class, null, queryParam, null, null);

        JUFUMatchConditions annotation = createMatchConditions(
            new JUFUCondition[]{cond1, cond2},
            EvaluationMode.FAIL_FAST
        );

        Condition parsed = parser.parse(annotation);

        assertNotNull(parsed);
        assertTrue(parsed instanceof ConditionGroup);
    }

    // Helper methods to create annotation instances
    private JUFUHeader createHeaderAnnotation(String name, String equals, String contains,
                                               String startsWith, String endsWith, String regex, boolean ignoreCase) {
        return new JUFUHeader() {
            public String name() { return name; }
            public String equals() { return equals; }
            public String contains() { return contains; }
            public String startsWith() { return startsWith; }
            public String endsWith() { return endsWith; }
            public String regex() { return regex; }
            public boolean ignoreCase() { return ignoreCase; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUHeader.class; }
        };
    }

    private JUFUQueryParam createQueryParamAnnotation(String name, String equals, String contains,
                                                       String startsWith, String endsWith, String regex, boolean ignoreCase) {
        return new JUFUQueryParam() {
            public String name() { return name; }
            public String equals() { return equals; }
            public String contains() { return contains; }
            public String startsWith() { return startsWith; }
            public String endsWith() { return endsWith; }
            public String regex() { return regex; }
            public boolean ignoreCase() { return ignoreCase; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUQueryParam.class; }
        };
    }

    private JUFUCondition createCondition(Class<? extends Condition> value, JUFUHeader header,
                                          JUFUQueryParam queryParam, JUFUJsonPath jsonPath,
                                          JUFUJsonExactMatch jsonExactMatch) {
        return new JUFUCondition() {
            public Class<? extends Condition> value() { return value; }
            public JUFUHeader header() { return header != null ? header : createEmptyHeader(); }
            public JUFUQueryParam queryParam() { return queryParam != null ? queryParam : createEmptyQueryParam(); }
            public JUFUJsonPath jsonPath() { return jsonPath != null ? jsonPath : createEmptyJsonPath(); }
            public JUFUJsonExactMatch jsonExactMatch() { return jsonExactMatch != null ? jsonExactMatch : createEmptyJsonExactMatch(); }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUCondition.class; }
        };
    }

    private JUFUMatchConditions createMatchConditions(JUFUCondition[] value, EvaluationMode mode) {
        return new JUFUMatchConditions() {
            public JUFUCondition[] value() { return value; }
            public EvaluationMode mode() { return mode; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUMatchConditions.class; }
        };
    }

    private JUFUHeader createEmptyHeader() {
        return createHeaderAnnotation("", "", "", "", "", "", false);
    }

    private JUFUQueryParam createEmptyQueryParam() {
        return createQueryParamAnnotation("", "", "", "", "", "", false);
    }

    private JUFUJsonPath createEmptyJsonPath() {
        return new JUFUJsonPath() {
            public String path() { return ""; }
            public String equals() { return ""; }
            public String contains() { return ""; }
            public String startsWith() { return ""; }
            public String endsWith() { return ""; }
            public String regex() { return ""; }
            public boolean ignoreCase() { return false; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUJsonPath.class; }
        };
    }

    private JUFUJsonExactMatch createEmptyJsonExactMatch() {
        return new JUFUJsonExactMatch() {
            public String template() { return ""; }
            public String[] fields() { return new String[0]; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUJsonExactMatch.class; }
        };
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=AnnotationConditionParserTest`
Expected: Compilation error - AnnotationConditionParser class not found

**Step 3: Implement AnnotationConditionParser (basic structure)**

Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/AnnotationConditionParser.java`

```java
package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses JUFU annotations into Condition objects.
 */
public class AnnotationConditionParser {

    public Condition parse(JUFUMatchConditions annotation) {
        JUFUCondition[] conditions = annotation.value();
        List<Condition> parsedConditions = new ArrayList<>();

        for (JUFUCondition condition : conditions) {
            Condition parsed = parse(condition);
            if (parsed != null) {
                parsedConditions.add(parsed);
            }
        }

        if (parsedConditions.isEmpty()) {
            throw new IllegalArgumentException("No valid conditions found in @JUFUMatchConditions");
        }

        if (parsedConditions.size() == 1) {
            return parsedConditions.get(0);
        }

        // Multiple conditions - combine with AND
        return ConditionGroup.and(parsedConditions.toArray(new Condition[0]));
    }

    public Condition parse(JUFUCondition annotation) {
        List<Condition> conditions = new ArrayList<>();

        // Parse class reference
        if (annotation.value() != Condition.class) {
            try {
                conditions.add(annotation.value().getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "Failed to instantiate condition class: " + annotation.value().getName(), e);
            }
        }

        // Parse inline header
        JUFUHeader header = annotation.header();
        if (!header.name().isEmpty()) {
            conditions.add(parseHeader(header));
        }

        // Parse inline query param
        JUFUQueryParam queryParam = annotation.queryParam();
        if (!queryParam.name().isEmpty()) {
            conditions.add(parseQueryParam(queryParam));
        }

        // Parse inline JSON path
        JUFUJsonPath jsonPath = annotation.jsonPath();
        if (!jsonPath.path().isEmpty()) {
            conditions.add(parseJsonPath(jsonPath));
        }

        // Parse inline JSON exact match
        JUFUJsonExactMatch jsonExactMatch = annotation.jsonExactMatch();
        if (!jsonExactMatch.template().isEmpty()) {
            conditions.add(parseJsonExactMatch(jsonExactMatch));
        }

        if (conditions.isEmpty()) {
            throw new IllegalArgumentException("@JUFUCondition must specify at least one condition");
        }

        if (conditions.size() == 1) {
            return conditions.get(0);
        }

        // Multiple inline conditions - combine with AND
        return ConditionGroup.and(conditions.toArray(new Condition[0]));
    }

    private Condition parseHeader(JUFUHeader header) {
        MatchOperation operation = determineOperation(
            header.equals(), header.contains(), header.startsWith(), header.endsWith(), header.regex()
        );
        String expectedValue = getExpectedValue(
            header.equals(), header.contains(), header.startsWith(), header.endsWith(), header.regex()
        );

        return new HeaderCondition(header.name(), expectedValue, operation, header.ignoreCase());
    }

    private Condition parseQueryParam(JUFUQueryParam queryParam) {
        MatchOperation operation = determineOperation(
            queryParam.equals(), queryParam.contains(), queryParam.startsWith(),
            queryParam.endsWith(), queryParam.regex()
        );
        String expectedValue = getExpectedValue(
            queryParam.equals(), queryParam.contains(), queryParam.startsWith(),
            queryParam.endsWith(), queryParam.regex()
        );

        return new QueryParamCondition(queryParam.name(), expectedValue, operation, queryParam.ignoreCase());
    }

    private Condition parseJsonPath(JUFUJsonPath jsonPath) {
        MatchOperation operation = determineOperation(
            jsonPath.equals(), jsonPath.contains(), jsonPath.startsWith(),
            jsonPath.endsWith(), jsonPath.regex()
        );
        String expectedValue = getExpectedValue(
            jsonPath.equals(), jsonPath.contains(), jsonPath.startsWith(),
            jsonPath.endsWith(), jsonPath.regex()
        );

        return new JsonPathCondition(jsonPath.path(), expectedValue, operation, jsonPath.ignoreCase());
    }

    private Condition parseJsonExactMatch(JUFUJsonExactMatch jsonExactMatch) {
        return new JsonExactMatchCondition(jsonExactMatch.template(), jsonExactMatch.fields());
    }

    private MatchOperation determineOperation(String equals, String contains, String startsWith,
                                               String endsWith, String regex) {
        if (!equals.isEmpty()) return MatchOperation.EQUALS;
        if (!contains.isEmpty()) return MatchOperation.CONTAINS;
        if (!startsWith.isEmpty()) return MatchOperation.STARTS_WITH;
        if (!endsWith.isEmpty()) return MatchOperation.ENDS_WITH;
        if (!regex.isEmpty()) return MatchOperation.REGEX;

        throw new IllegalArgumentException("No matching operation specified");
    }

    private String getExpectedValue(String equals, String contains, String startsWith,
                                      String endsWith, String regex) {
        if (!equals.isEmpty()) return equals;
        if (!contains.isEmpty()) return contains;
        if (!startsWith.isEmpty()) return startsWith;
        if (!endsWith.isEmpty()) return endsWith;
        if (!regex.isEmpty()) return regex;

        throw new IllegalArgumentException("No expected value specified");
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=AnnotationConditionParserTest`
Expected: PASS - All 3 tests passing

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/AnnotationConditionParser.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/AnnotationConditionParserTest.java
git commit -m "feat(aop): add AnnotationConditionParser for annotation processing"
```

---

### Task 2.2: Add Spring AOP Dependency

**Files:**
- Modify: `request-utils/pom.xml`

**Step 1: Add Spring AOP dependency to pom.xml**

Add after the JSONPath dependency:

```xml
<!-- AOP support for annotation-based condition matching -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**Step 2: Build to verify dependency downloads**

Run: `cd request-utils && ./mvnw clean compile`
Expected: BUILD SUCCESS with Spring AOP dependency downloaded

**Step 3: Commit**

```bash
git add request-utils/pom.xml
git commit -m "build: add Spring AOP starter for aspect support"
```

---

### Task 2.3: ConditionMatchingAspect Implementation

**Files:**
- Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionMatchingAspect.java`
- Test: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionMatchingAspectTest.java`

**Step 1: Write test for aspect**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionMatchingAspectTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.core.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConditionMatchingAspectTest {

    @Test
    void shouldProceedWhenConditionMatches() throws Throwable {
        RequestConditionMatcher matcher = Mockito.mock(RequestConditionMatcher.class);
        when(matcher.evaluate(any(Condition.class), any(HttpServletRequest.class)))
            .thenReturn(ConditionResult.success());

        ConditionMatchingAspect aspect = new ConditionMatchingAspect(matcher, new AnnotationConditionParser());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "secret");

        // Should not throw exception
        assertDoesNotThrow(() -> {
            aspect.checkConditions(createJoinPoint(request), createMatchConditions());
        });
    }

    @Test
    void shouldThrowExceptionWhenConditionFails() throws Throwable {
        RequestConditionMatcher matcher = Mockito.mock(RequestConditionMatcher.class);
        ConditionFailure failure = ConditionFailure.builder()
            .message("Header 'X-Api-Key' not found")
            .build();
        when(matcher.evaluate(any(Condition.class), any(HttpServletRequest.class)))
            .thenReturn(ConditionResult.failure(failure));

        ConditionMatchingAspect aspect = new ConditionMatchingAspect(matcher, new AnnotationConditionParser());

        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThrows(ConditionNotMetException.class, () -> {
            aspect.checkConditions(createJoinPoint(request), createMatchConditions());
        });
    }

    private org.aspectj.lang.JoinPoint createJoinPoint(HttpServletRequest request) {
        org.aspectj.lang.JoinPoint joinPoint = Mockito.mock(org.aspectj.lang.JoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{request});
        return joinPoint;
    }

    private JUFUMatchConditions createMatchConditions() {
        JUFUHeader header = new JUFUHeader() {
            public String name() { return "X-Api-Key"; }
            public String equals() { return "secret"; }
            public String contains() { return ""; }
            public String startsWith() { return ""; }
            public String endsWith() { return ""; }
            public String regex() { return ""; }
            public boolean ignoreCase() { return false; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUHeader.class; }
        };

        JUFUCondition condition = new JUFUCondition() {
            public Class<? extends Condition> value() { return Condition.class; }
            public JUFUHeader header() { return header; }
            public JUFUQueryParam queryParam() { return null; }
            public JUFUJsonPath jsonPath() { return null; }
            public JUFUJsonExactMatch jsonExactMatch() { return null; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUCondition.class; }
        };

        return new JUFUMatchConditions() {
            public JUFUCondition[] value() { return new JUFUCondition[]{condition}; }
            public EvaluationMode mode() { return EvaluationMode.FAIL_FAST; }
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return JUFUMatchConditions.class; }
        };
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionMatchingAspectTest`
Expected: Compilation error - ConditionMatchingAspect class not found

**Step 3: Implement ConditionMatchingAspect**

Create: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionMatchingAspect.java`

```java
package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.annotations.JUFUMatchConditions;
import com.cleveloper.jufu.requestutils.condition.core.*;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Spring AOP aspect that intercepts methods annotated with @JUFUMatchConditions
 * and evaluates conditions before method execution.
 */
@Aspect
@Component
@Order(100)
public class ConditionMatchingAspect {

    private final RequestConditionMatcher matcher;
    private final AnnotationConditionParser parser;

    public ConditionMatchingAspect(RequestConditionMatcher matcher, AnnotationConditionParser parser) {
        this.matcher = matcher;
        this.parser = parser;
    }

    @Before("@annotation(matchConditions)")
    public void checkConditions(JoinPoint joinPoint, JUFUMatchConditions matchConditions) {
        HttpServletRequest request = extractRequest(joinPoint);

        if (request == null) {
            throw new IllegalStateException(
                "No HttpServletRequest found in method arguments or RequestContextHolder. " +
                "@JUFUMatchConditions requires HttpServletRequest to be available."
            );
        }

        Condition condition = parser.parse(matchConditions);
        ConditionResult result = matcher.evaluate(condition, request);

        if (!result.isMatched()) {
            throw new ConditionNotMetException(result);
        }
    }

    private HttpServletRequest extractRequest(JoinPoint joinPoint) {
        // First, try to find HttpServletRequest in method arguments
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof HttpServletRequest) {
                return (HttpServletRequest) arg;
            }
        }

        // Fallback: try RequestContextHolder
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            return attributes.getRequest();
        }

        return null;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=ConditionMatchingAspectTest`
Expected: PASS - All 2 tests passing

**Step 5: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionMatchingAspect.java request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/aop/ConditionMatchingAspectTest.java
git commit -m "feat(aop): add ConditionMatchingAspect for declarative condition evaluation"
```

---

### Task 2.4: Update Auto-Configuration for AOP

**Files:**
- Modify: `request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfiguration.java`

**Step 1: Read current auto-configuration**

Run: `cat request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfiguration.java`

**Step 2: Update auto-configuration to include AOP beans**

Modify the file to add AnnotationConditionParser and ConditionMatchingAspect beans:

```java
package com.cleveloper.jufu.requestutils.condition.config;

import com.cleveloper.jufu.requestutils.condition.aop.AnnotationConditionParser;
import com.cleveloper.jufu.requestutils.condition.aop.ConditionMatchingAspect;
import com.cleveloper.jufu.requestutils.condition.core.RequestConditionMatcher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for request condition matching.
 */
@AutoConfiguration
public class ConditionMatcherAutoConfiguration {

    @Bean
    public RequestConditionMatcher requestConditionMatcher() {
        return new RequestConditionMatcher();
    }

    @Bean
    public AnnotationConditionParser annotationConditionParser() {
        return new AnnotationConditionParser();
    }

    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public ConditionMatchingAspect conditionMatchingAspect(
            RequestConditionMatcher matcher,
            AnnotationConditionParser parser) {
        return new ConditionMatchingAspect(matcher, parser);
    }
}
```

**Step 3: Run tests to verify configuration**

Run: `cd request-utils && ./mvnw test`
Expected: All tests passing (should be 109+ tests)

**Step 4: Commit**

```bash
git add request-utils/src/main/java/com/cleveloper/jufu/requestutils/condition/config/ConditionMatcherAutoConfiguration.java
git commit -m "feat(config): add AOP beans to auto-configuration"
```

---

## Phase 3: Integration Testing and Documentation

### Task 3.1: Integration Test for AOP

**Files:**
- Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/integration/AopIntegrationTest.java`

**Step 1: Write integration test**

Create: `request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/integration/AopIntegrationTest.java`

```java
package com.cleveloper.jufu.requestutils.condition.integration;

import com.cleveloper.jufu.requestutils.condition.annotations.*;
import com.cleveloper.jufu.requestutils.condition.aop.ConditionNotMetException;
import com.cleveloper.jufu.requestutils.condition.core.EvaluationMode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AopIntegrationTest.TestController.class)
@ComponentScan(basePackages = "com.cleveloper.jufu.requestutils.condition")
class AopIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowRequestWhenConditionMatches() throws Exception {
        mockMvc.perform(get("/api/premium")
                .header("X-User-Type", "premium")
                .param("version", "v2"))
            .andExpect(status().isOk())
            .andExpect(content().string("Premium access granted"));
    }

    @Test
    void shouldRejectRequestWhenHeaderConditionFails() throws Exception {
        mockMvc.perform(get("/api/premium")
                .header("X-User-Type", "basic")
                .param("version", "v2"))
            .andExpect(status().isInternalServerError()); // ConditionNotMetException thrown
    }

    @Test
    void shouldRejectRequestWhenQueryParamConditionFails() throws Exception {
        mockMvc.perform(get("/api/premium")
                .header("X-User-Type", "premium")
                .param("version", "v1"))
            .andExpect(status().isInternalServerError());
    }

    @Controller
    static class TestController {

        @GetMapping("/api/premium")
        @ResponseBody
        @JUFUMatchConditions({
            @JUFUCondition(header = @JUFUHeader(name = "X-User-Type", equals = "premium")),
            @JUFUCondition(queryParam = @JUFUQueryParam(name = "version", startsWith = "v2"))
        })
        public String premiumEndpoint(HttpServletRequest request) {
            return "Premium access granted";
        }
    }
}
```

**Step 2: Run test to verify it passes**

Run: `cd request-utils && ./mvnw test -Dtest=AopIntegrationTest`
Expected: PASS - All 3 tests passing

**Step 3: Commit**

```bash
git add request-utils/src/test/java/com/cleveloper/jufu/requestutils/condition/integration/AopIntegrationTest.java
git commit -m "test(integration): add AOP integration tests"
```

---

### Task 3.2: Update README with AOP Usage

**Files:**
- Modify: `request-utils/README.md`

**Step 1: Add AOP usage section to README**

Add a new section after "Custom Conditions" (around line 450):

```markdown
### Annotation-Based (AOP) Usage

Use `@JUFUMatchConditions` annotation to declaratively validate requests:

```java
import com.cleveloper.jufu.requestutils.condition.annotations.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiController {

    @GetMapping("/api/premium/feature")
    @JUFUMatchConditions({
        @JUFUCondition(header = @JUFUHeader(name = "X-User-Type", equals = "premium")),
        @JUFUCondition(queryParam = @JUFUQueryParam(name = "version", startsWith = "v2"))
    })
    public ResponseEntity<?> premiumFeature(HttpServletRequest request) {
        // Method only executes if both conditions pass
        // Otherwise ConditionNotMetException is thrown automatically
        return ResponseEntity.ok("Premium feature access");
    }

    @PostMapping("/api/data")
    @JUFUMatchConditions(
        value = {
            @JUFUCondition(header = @JUFUHeader(name = "Content-Type", equals = "application/json", ignoreCase = true)),
            @JUFUCondition(jsonPath = @JUFUJsonPath(path = "$.user.email", contains = "@company.com"))
        },
        mode = EvaluationMode.COLLECT_ALL  // Collect all failures for detailed error reporting
    )
    public ResponseEntity<?> processData(HttpServletRequest request) {
        // Both header and JSON payload conditions must pass
        return ResponseEntity.ok("Data processed");
    }
}
```

**Annotation Features:**
- **Method-level**: Apply to individual endpoints
- **Class-level**: Apply to all methods in a controller (coming soon)
- **Inline conditions**: Define conditions directly in annotation
- **Class references**: Reference custom Condition implementations
- **Hybrid mode**: Combine inline and class reference in same annotation
- **Evaluation modes**: FAIL_FAST (default) or COLLECT_ALL

**How It Works:**
1. Spring AOP aspect intercepts annotated methods before execution
2. Extracts HttpServletRequest from method arguments or RequestContextHolder
3. Parses annotations into Condition objects
4. Evaluates conditions using RequestConditionMatcher
5. If matched: proceeds with method execution
6. If not matched: throws ConditionNotMetException with failure details
```

**Step 2: Update Features section to mark AOP as available**

Find the Features section and update:

Change:
```markdown
🚧 **Coming Soon:**
- Annotation-based AOP integration (`@JUFUMatchConditions`)
```

To:
```markdown
✅ **Recently Added:**
- JSON payload matching (JSONPath and exact field matching)
- Annotation-based AOP integration (`@JUFUMatchConditions`)
```

**Step 3: Update Roadmap section**

Move Phase 2 items to completed:

```markdown
### Phase 2 (Completed) ✅
- JSON payload matching
  - JSONPath-based field extraction
  - Exact field matching
- Annotation-based AOP integration
  - `@JUFUMatchConditions` annotation
  - Method-level declarations
  - Inline condition definitions
```

**Step 4: Commit**

```bash
git add request-utils/README.md
git commit -m "docs: update README with AOP usage and JSON matching examples"
```

---

## Summary

This implementation plan provides complete JSON matching and AOP integration for the request condition matcher utility. The plan is organized into 3 phases:

1. **Phase 1: JSON Matching** - JSONPath and exact field matching (5 tasks)
2. **Phase 2: AOP Integration** - Annotation parsing and aspect implementation (4 tasks)
3. **Phase 3: Integration & Docs** - Integration testing and documentation (2 tasks)

Each task follows TDD principles: write test → verify failure → implement → verify pass → commit.

**Dependencies added:**
- Jayway JSONPath 2.9.0 (optional)
- Spring Boot Starter AOP

**New components:**
- JsonPathCondition - Extract and match JSON fields using JSONPath
- JsonExactMatchCondition - Match specific fields against template
- AnnotationConditionParser - Convert annotations to Condition objects
- ConditionMatchingAspect - AOP aspect for declarative validation
- JSON annotations (@JUFUJsonPath, @JUFUJsonExactMatch)

**Total estimated tasks:** 11 tasks
**Total estimated time:** 50-100 minutes for complete implementation
