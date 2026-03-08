# Request Condition Matcher Design

**Date:** 2026-03-08
**Module:** request-utils
**Purpose:** Request filtering/routing utility that matches configured conditions for headers, query parameters, and JSON payload values

## Overview

This design document describes a flexible request condition matching utility for the `request-utils` module. The utility evaluates HTTP request characteristics against configured conditions and returns detailed success/failure results. It supports both annotation-based declarative usage (via AOP) and programmatic usage (via direct service injection).

## Requirements Summary

- Match conditions against HTTP headers, query parameters, and JSON payload fields
- Support string operations: equals, contains, starts with, ends with (case-sensitive and insensitive), and regex patterns
- Combine conditions using AND/OR logic with support for nested groups
- Two usage modes: annotation-based (AOP) and programmatic (direct service call)
- On failure, throw exception with detailed information about which conditions failed
- Configurable evaluation modes: fail-fast (stop at first failure) or collect-all (evaluate all and report all failures)
- JSON matching via nested field access (JSONPath) and exact field matching
- Annotations support both inline simple conditions and references to complex condition classes

---

## Section 1: Core Components

The architecture centers on a framework-agnostic matching engine with Spring integration layers on top.

### Core Components

#### 1. RequestConditionMatcher

The main service that evaluates conditions against HTTP requests. It accepts a `ConditionGroup` (representing all configured conditions) and a `RequestContext` (abstraction over Spring's HttpServletRequest), returning a `ConditionResult`.

#### 2. Condition Interface

Simple contract with a single method: `ConditionResult evaluate(RequestContext context)`. All condition types implement this interface.

#### 3. ConditionResult

Immutable result object containing:
- `boolean matched` - Whether the condition passed
- `List<ConditionFailure> failures` - Details of which conditions failed (empty if matched)
- `EvaluationMode mode` - Whether it was FAIL_FAST or COLLECT_ALL

#### 4. ConditionGroup

Represents a group of conditions with their logical operator (AND/OR). Can contain both individual conditions and nested groups to support `(A OR B) AND C` structures.

#### 5. RequestContext

Framework-agnostic wrapper providing access to:
- Headers (multi-value map)
- Query parameters (multi-value map)
- JSON payload (parsed as JsonNode for path-based access)
- Raw request body (for exact JSON matching)

The matcher itself has no Spring dependencies - it operates purely on the `Condition` interface and `RequestContext` abstraction.

---

## Section 2: Annotation Structure

The annotation layer provides a declarative way to define conditions while supporting both inline simple conditions and complex condition class references. All annotations are prefixed with `JUFU` for clear separation.

### Primary Annotations

#### @JUFUMatchConditions

Container annotation that holds multiple `@JUFUCondition` entries. Supports method-level and class-level placement. Can specify evaluation mode:

```java
@JUFUMatchConditions(
    value = { @JUFUCondition(...), @JUFUCondition(...) },
    mode = EvaluationMode.FAIL_FAST  // or COLLECT_ALL
)
```

#### @JUFUCondition

Individual condition definition. Supports two modes:
- **Class reference mode**: `@JUFUCondition(MyConditionClass.class)`
- **Inline mode**: `@JUFUCondition(headers = @JUFUHeader(...), queryParams = @JUFUQueryParam(...))`
- **Hybrid**: Can combine both in the same `@JUFUCondition`

#### @JUFUHeader

Inline header matching with attributes:
- `name` - Header name (required)
- `equals`, `contains`, `startsWith`, `endsWith` - String operations
- `regex` - Pattern matching
- `ignoreCase` - Boolean flag (default: false)

#### @JUFUQueryParam

Same structure as `@JUFUHeader` but for query parameters.

#### @JUFUJsonPath

JSON field matching with attributes:
- `path` - JSONPath expression (e.g., `"$.user.profile.email"`)
- Same matching operations as Header/QueryParam

#### @JUFUJsonExactMatch

Exact field matching:
- `template` - JSON string to match against
- `fields` - Array of field paths that must match exactly

### Condition Composition

Multiple `@JUFUCondition` entries within `@JUFUMatchConditions` are combined with AND logic by default. OR groups are created using `@JUFUOrGroup` wrapper. AND groups can be explicitly created using `@JUFUAndGroup`.

**Example:**

```java
@JUFUMatchConditions({
    @JUFUCondition(headers = @JUFUHeader(name="X-Type", equals="premium")),
    @JUFUCondition(ComplexUserValidation.class),
    @JUFUCondition(queryParams = @JUFUQueryParam(name="version", regex="v[2-9]"))
})
public void handlePremiumRequest(HttpServletRequest request) {
    // method implementation
}
```

---

## Section 3: Condition Evaluation Logic

The `RequestConditionMatcher` evaluates conditions using a recursive algorithm that respects the AND/OR grouping structure.

### Evaluation Flow

#### 1. Parse Request

Convert incoming `HttpServletRequest` into `RequestContext`, extracting headers, query params, and parsing JSON payload if Content-Type is application/json.

#### 2. Build Condition Tree

From annotations (via aspect) or programmatic API, construct a `ConditionGroup` tree representing the logical structure.

#### 3. Evaluate Recursively

- **For AND groups**: Evaluate each condition/subgroup. In FAIL_FAST mode, stop at first failure. In COLLECT_ALL mode, evaluate all and accumulate failures.
- **For OR groups**: Evaluate each condition/subgroup until one succeeds (short-circuit success). In COLLECT_ALL mode, evaluate all to report which ones passed.
- **Leaf conditions**: Execute the specific matcher (header, query param, JSON path, etc.)

#### 4. Build Result

Create `ConditionResult` with:
- Overall matched status
- List of `ConditionFailure` objects, each containing:
  - Condition type (e.g., "Header", "JsonPath")
  - Expected value/operation
  - Actual value found
  - Descriptive message

### Evaluation Modes

- **FAIL_FAST**: Returns immediately on first failure (better performance)
- **COLLECT_ALL**: Evaluates all conditions and collects all failures (better debugging)

Mode can be set per annotation, with a global default configurable via Spring properties.

---

## Section 4: JSON Matching

JSON matching supports two distinct modes: path-based field extraction and exact field matching.

### Path-Based Matching (@JUFUJsonPath)

Uses JSONPath expressions to extract field values from the request payload, then applies string operations:

```java
@JUFUCondition(
    jsonPath = @JUFUJsonPath(
        path = "$.user.profile.email",
        contains = "@example.com"
    )
)
```

**Implementation** uses a JSONPath library (Jayway JsonPath) to:
1. Parse the request body as JSON (cached in `RequestContext`)
2. Evaluate the path expression
3. Convert result to string (handle nulls gracefully)
4. Apply the specified string operation (equals, contains, regex, etc.)

Supports nested object access (`$.user.address.city`) and array notation (`$.items[0].name`), but not complex queries - keeps it simple and predictable.

### Exact Field Matching (@JUFUJsonExactMatch)

Compares specific fields in the request payload against a template JSON string:

```java
@JUFUCondition(
    jsonExactMatch = @JUFUJsonExactMatch(
        template = "{\"type\": \"premium\", \"region\": \"US\"}",
        fields = {"type", "region"}
    )
)
```

**Implementation:**
1. Parse both the template and request payload as JSON
2. For each field in the `fields` array, extract values from both JSONs
3. Compare values for exact equality (deep comparison for nested objects)
4. Fails if any field doesn't match or is missing in the request

This is useful for matching specific payload structures without caring about extra fields in the request.

### Optional Dependency

The JSONPath library (Jayway JsonPath) will be marked as an **optional dependency** in the `pom.xml`. JSON matching features will only be available when the library is present on the classpath.

**Implementation approach:**
- Use `@ConditionalOnClass(JsonPath.class)` for JSON-related condition beans
- Throw clear `UnsupportedOperationException` with dependency instructions if `@JUFUJsonPath` or `@JUFUJsonExactMatch` are used without the library
- Document the optional dependency in README

---

## Section 5: AOP Integration

The AOP layer intercepts methods annotated with `@JUFUMatchConditions` and evaluates conditions before method execution.

### Aspect Design

#### ConditionMatchingAspect

Spring aspect with:
- **Pointcut**: `@annotation(JUFUMatchConditions) || @within(JUFUMatchConditions)`
- Runs before method execution
- **Order**: Configurable via `@Order`, defaults to high priority (runs early)

#### Execution Flow

1. Extract `HttpServletRequest` from method arguments (must be present) or from `RequestContextHolder`
2. Read `@JUFUMatchConditions` annotation (method-level overrides class-level)
3. Convert annotations to `ConditionGroup` using `AnnotationConditionParser`
4. Call `RequestConditionMatcher.evaluate(conditionGroup, requestContext)`
5. If matched: Proceed with method execution
6. If not matched: Throw `ConditionNotMetException`

### ConditionNotMetException

Custom exception containing:
- `ConditionResult result` - Full evaluation result with failure details
- Formatted message listing all failures
- HTTP-friendly structure for error responses

### Exception Handler

Optional `@ControllerAdvice` that catches `ConditionNotMetException` and converts it to appropriate HTTP response (e.g., 400 Bad Request or 403 Forbidden with failure details).

---

## Section 6: Programmatic Usage

The `RequestConditionMatcher` can be injected and used directly in service classes, controllers, or anywhere programmatic evaluation is needed.

### Direct Service Injection

```java
@Service
public class RequestRouter {
    private final RequestConditionMatcher matcher;

    @Autowired
    public RequestRouter(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    public void routeRequest(HttpServletRequest request) {
        ConditionGroup conditions = ConditionGroup.builder()
            .and(new HeaderCondition("X-Api-Key", equals("premium")))
            .and(new QueryParamCondition("version", regex("v[2-9]")))
            .build();

        ConditionResult result = matcher.evaluate(conditions, request);

        if (!result.isMatched()) {
            throw new ConditionNotMetException(result);
        }
        // proceed with routing logic
    }
}
```

### Builder API for Conditions

Provides fluent API for creating conditions programmatically:
- `ConditionGroup.builder()` - Start building a group
- `.and(Condition)` - Add AND condition
- `.or(Condition)` - Add OR condition
- `.andGroup(Consumer<Builder>)` - Create nested AND group
- `.orGroup(Consumer<Builder>)` - Create nested OR group
- `.mode(EvaluationMode)` - Set evaluation mode

### Pre-built Condition Classes

- `HeaderCondition`, `QueryParamCondition`, `JsonPathCondition`, `JsonExactMatchCondition`
- Each accepts the field name and a `MatchOperation` enum (EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, REGEX)
- Supports case-insensitive flag

### Custom Conditions

Users can implement the `Condition` interface for complex business logic:

```java
public class CustomUserValidation implements Condition {
    @Override
    public ConditionResult evaluate(RequestContext context) {
        // custom validation logic here
        return ConditionResult.success();
        // or ConditionResult.failure("reason");
    }
}
```

---

## Section 7: Testing Strategy

The modular design enables comprehensive testing at multiple levels.

### Unit Testing - Individual Conditions

Test each condition type in isolation using mock `RequestContext`:
- `HeaderConditionTest` - Test all string operations (equals, contains, regex, etc.) and case sensitivity
- `QueryParamConditionTest` - Same operations for query parameters
- `JsonPathConditionTest` - Test path extraction and matching (requires JSONPath library)
- `JsonExactMatchConditionTest` - Test exact field matching logic

Use test fixtures with known request data to verify each matcher correctly evaluates conditions and produces accurate failure messages.

### Unit Testing - Condition Groups

Test `ConditionGroup` evaluation logic:
- AND groups: All must pass, failure collection in COLLECT_ALL mode
- OR groups: Short-circuit on success, failure handling
- Nested groups: Complex boolean expressions like `(A OR B) AND (C OR D)`
- Mode switching: FAIL_FAST stops early, COLLECT_ALL gathers all failures

### Integration Testing - Request Matcher

Test `RequestConditionMatcher` with real `HttpServletRequest` mocks:
- Multiple conditions across headers, params, and JSON
- Mixed inline and custom condition classes
- Evaluation mode behavior
- Error messages contain correct failure details

### Integration Testing - AOP Aspect

Spring Boot test with `@WebMvcTest`:
- Annotate test controller methods with `@JUFUMatchConditions`
- Send requests via `MockMvc`
- Verify aspect intercepts correctly
- Verify `ConditionNotMetException` thrown with proper details
- Test class-level and method-level annotation merging

### Testing Custom Conditions

Provide test utilities:
- `MockRequestContext.builder()` - Fluent API for creating test contexts
- `ConditionTestUtils` - Helper assertions for `ConditionResult` validation

---

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Code                              │
│  (Controllers, Services with @JUFUMatchConditions or         │
│   direct RequestConditionMatcher injection)                  │
└───────────────────┬─────────────────────────────────────────┘
                    │
        ┌───────────┴──────────┐
        │                      │
        ▼                      ▼
┌──────────────────┐   ┌──────────────────────┐
│ AOP Integration  │   │ Programmatic Usage   │
│                  │   │                      │
│ - Aspect         │   │ - Direct injection   │
│ - Annotation     │   │ - Builder API        │
│   Parser         │   │                      │
└────────┬─────────┘   └──────────┬───────────┘
         │                        │
         └────────┬───────────────┘
                  ▼
     ┌────────────────────────────┐
     │ RequestConditionMatcher    │
     │ (Core Service)             │
     └────────────┬───────────────┘
                  │
                  ▼
     ┌────────────────────────────┐
     │ ConditionGroup             │
     │ (AND/OR Tree)              │
     └────────────┬───────────────┘
                  │
         ┌────────┴────────┐
         ▼                 ▼
┌─────────────────┐  ┌──────────────────┐
│ Leaf Conditions │  │ Nested Groups    │
│                 │  │                  │
│ - Header        │  │ - AND Group      │
│ - QueryParam    │  │ - OR Group       │
│ - JsonPath      │  │                  │
│ - JsonExact     │  │                  │
│ - Custom        │  │                  │
└─────────────────┘  └──────────────────┘
```

---

## Package Structure

```
com.cleveloper.jufu.requestutils.condition
├── core
│   ├── Condition.java                      # Core interface
│   ├── ConditionGroup.java                 # Group with AND/OR logic
│   ├── ConditionResult.java                # Evaluation result
│   ├── ConditionFailure.java               # Failure details
│   ├── EvaluationMode.java                 # FAIL_FAST / COLLECT_ALL
│   ├── RequestContext.java                 # Request abstraction
│   └── RequestConditionMatcher.java        # Main matching service
├── matchers
│   ├── HeaderCondition.java                # Header matcher
│   ├── QueryParamCondition.java            # Query param matcher
│   ├── JsonPathCondition.java              # JSONPath matcher
│   ├── JsonExactMatchCondition.java        # Exact JSON matcher
│   └── MatchOperation.java                 # EQUALS, CONTAINS, etc.
├── annotations
│   ├── JUFUMatchConditions.java            # Container annotation
│   ├── JUFUCondition.java                  # Single condition
│   ├── JUFUHeader.java                     # Header inline config
│   ├── JUFUQueryParam.java                 # Query param inline config
│   ├── JUFUJsonPath.java                   # JSONPath inline config
│   ├── JUFUJsonExactMatch.java             # Exact match inline config
│   ├── JUFUAndGroup.java                   # AND group wrapper
│   └── JUFUOrGroup.java                    # OR group wrapper
├── aop
│   ├── ConditionMatchingAspect.java        # AOP aspect
│   ├── AnnotationConditionParser.java      # Annotation → Condition converter
│   └── ConditionNotMetException.java       # Exception on failure
├── builder
│   └── ConditionGroupBuilder.java          # Fluent builder API
└── config
    └── ConditionMatcherAutoConfiguration.java  # Spring Boot auto-config
```

---

## Configuration Properties

```properties
# Global evaluation mode (default: FAIL_FAST)
jufu.condition-matcher.evaluation-mode=FAIL_FAST

# Enable/disable AOP aspect (default: true)
jufu.condition-matcher.aop.enabled=true

# AOP aspect order (default: 100)
jufu.condition-matcher.aop.order=100

# Enable default exception handler (default: true)
jufu.condition-matcher.exception-handler.enabled=true
```

---

## Implementation Notes

1. **Thread Safety**: `RequestConditionMatcher` should be stateless and thread-safe for concurrent request processing.

2. **Performance**: Cache parsed JSON payloads in `RequestContext` to avoid re-parsing for multiple JSON conditions.

3. **Error Messages**: Provide clear, actionable failure messages (e.g., "Header 'X-Api-Key' expected to equal 'premium' but was 'basic'").

4. **Null Handling**: Define clear behavior for missing headers/params/JSON fields (typically treated as condition failure).

5. **Spring Boot Starter**: Package as a Spring Boot starter for easy integration with auto-configuration.

6. **Documentation**: Provide comprehensive JavaDoc and a usage guide with examples.

---

## Next Steps

1. Create implementation plan with detailed tasks
2. Set up git worktree for isolated development
3. Implement core components (Section 1)
4. Implement condition matchers (headers, params, JSON)
5. Implement annotation layer and AOP integration
6. Implement programmatic API and builder
7. Write comprehensive tests
8. Document usage examples
9. Create README with dependency information
