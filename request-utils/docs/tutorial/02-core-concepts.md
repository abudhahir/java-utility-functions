# Core Concepts

**Goal:** Understand the architecture and key components of request condition matching.

This guide explains the fundamental building blocks. After reading, you'll know when to use each component and how they work together.

---

## Architecture Overview

Request condition matching follows a simple flow:

```
HTTP Request → RequestContext → Condition → ConditionResult
                                    ↓
                            matched or failures
```

**Key insight:** Conditions are pure functions that take a `RequestContext` and return a `ConditionResult`. This makes them composable, testable, and reusable.

---

## The Four Key Interfaces

### 1. Condition

The core abstraction - a function that evaluates to true or false:

```java
@FunctionalInterface
public interface Condition {
    ConditionResult evaluate(RequestContext context);
}
```

**Built-in implementations:**
- `HeaderCondition` - Match HTTP headers
- `QueryParamCondition` - Match query parameters
- `JsonPathCondition` - Extract and match JSON fields
- `JsonExactMatchCondition` - Match specific JSON fields
- `ConditionGroup` - Combine conditions with AND/OR logic

**Key property:** Conditions are stateless and immutable. Create once, reuse everywhere.

```java
// Create once
private static final Condition PREMIUM_USER = new HeaderCondition(
    "X-User-Type", "premium", MatchOperation.EQUALS, false
);

// Reuse in multiple methods
public void method1(HttpServletRequest request) {
    ConditionResult result = matcher.evaluate(PREMIUM_USER, request);
}

public void method2(HttpServletRequest request) {
    ConditionResult result = matcher.evaluate(PREMIUM_USER, request);
}
```

---

### 2. ConditionResult

The outcome of evaluating a condition:

```java
public class ConditionResult {
    boolean isMatched();                    // Did all conditions pass?
    List<ConditionFailure> getFailures();   // Why did it fail? (empty if matched)
}
```

**Usage pattern:**

```java
ConditionResult result = matcher.evaluate(condition, request);

if (result.isMatched()) {
    // Success path
} else {
    // Failure path - examine failures for details
    for (ConditionFailure failure : result.getFailures()) {
        log.warn("Condition failed: {}", failure.getMessage());
    }
}
```

**Key insight:** `ConditionResult` never throws exceptions. It captures success or failure as data, making error handling explicit and composable.

---

### 3. ConditionFailure

Detailed information about why a condition failed:

```java
public class ConditionFailure {
    String getConditionType();    // "Header", "QueryParam", "JsonPath"
    String getFieldName();        // "X-Api-Key", "version", "$.user.email"
    String getOperation();        // "equals", "contains", "starts with"
    String getExpectedValue();    // "premium", "v2", "admin"
    String getActualValue();      // "basic", "v1", "[not present]"
    String getMessage();          // Human-readable description
}
```

**Example failure message:**
```
Header 'X-User-Type' expected to equal 'premium' but was 'basic'
```

**Why this matters:**
- Debugging: See exactly why a request was rejected
- Error responses: Return structured failures to clients
- Logging: Capture detailed audit trails

```java
if (!result.isMatched()) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("message", "Request validation failed");
    errorResponse.put("failures", result.getFailures().stream()
        .map(ConditionFailure::getMessage)
        .collect(Collectors.toList()));

    return ResponseEntity.badRequest().body(errorResponse);
}
```

---

### 4. RequestContext

Provides access to request data in a testable way:

```java
public interface RequestContext {
    String getHeader(String name);
    String getQueryParam(String name);
    Object getJsonBody();

    // Factory method
    static RequestContext from(HttpServletRequest request);
}
```

**Why context?** Decouples conditions from `HttpServletRequest`, making them:
- **Testable:** Mock `RequestContext` instead of servlet request
- **Flexible:** Use outside web layer if needed
- **Cacheable:** Request body parsed once, reused for multiple conditions

**Usage in custom conditions:**

```java
public class CustomCondition implements Condition {
    @Override
    public ConditionResult evaluate(RequestContext context) {
        String apiKey = context.getHeader("X-Api-Key");
        // ... evaluate logic
    }
}
```

---

## Match Operations

Five operations for string matching:

```java
public enum MatchOperation {
    EQUALS,        // Exact match: "premium" == "premium"
    CONTAINS,      // Substring: "Mobile" in "Mozilla/5.0 (Mobile)"
    STARTS_WITH,   // Prefix: "Bearer " at start of "Bearer token123"
    ENDS_WITH,     // Suffix: "/json" at end of "application/json"
    REGEX          // Pattern: "v\\d+\\.\\d+" matches "v2.0", "v3.5"
}
```

**When to use each:**

| Operation | Use Case | Example |
|-----------|----------|---------|
| `EQUALS` | Exact value match | API key validation, tenant ID |
| `CONTAINS` | Partial match | User-Agent detection, search |
| `STARTS_WITH` | Prefix match | Authorization headers, versioning |
| `ENDS_WITH` | Suffix match | Content-Type checking, file extensions |
| `REGEX` | Pattern match | Email validation, complex formats |

**Performance tip:** `EQUALS` > `STARTS_WITH`/`ENDS_WITH` > `CONTAINS` > `REGEX`. Use the simplest operation that works.

---

## Evaluation Modes

Control how conditions are evaluated:

```java
public enum EvaluationMode {
    FAIL_FAST,     // Stop at first failure (default)
    COLLECT_ALL    // Evaluate all, collect all failures
}
```

### FAIL_FAST Mode (Default)

**When to use:** Production, performance-critical paths

**Behavior:** Stops at the first failure

```java
Condition condition = ConditionGroup.builder()
    .and(condition1)  // If this fails...
    .and(condition2)  // ...this is never evaluated
    .and(condition3)
    .mode(EvaluationMode.FAIL_FAST)
    .build();
```

**Advantages:**
- ⚡ Faster (skips unnecessary checks)
- 🎯 Sufficient for most use cases
- 💰 Cheaper (fewer operations)

**Disadvantage:**
- Only see first failure (harder to debug multiple issues)

---

### COLLECT_ALL Mode

**When to use:** Development, debugging, detailed error reporting

**Behavior:** Evaluates all conditions, collects all failures

```java
Condition condition = ConditionGroup.builder()
    .and(condition1)  // Evaluated
    .and(condition2)  // Evaluated even if condition1 fails
    .and(condition3)  // Evaluated even if condition1 & 2 fail
    .mode(EvaluationMode.COLLECT_ALL)
    .build();
```

**Advantages:**
- 🔍 See all failures at once
- 🐛 Better debugging experience
- 📋 Complete validation reports

**Disadvantage:**
- 🐌 Slower (evaluates everything)

**Example: Form validation**

```java
// Check all required fields, report all missing ones
Condition formValidation = ConditionGroup.builder()
    .and(new HeaderCondition("Content-Type", "application/json", MatchOperation.EQUALS, true))
    .and(new JsonPathCondition("$.email", ".+@.+", MatchOperation.REGEX, false))
    .and(new JsonPathCondition("$.name", ".{3,}", MatchOperation.REGEX, false))
    .mode(EvaluationMode.COLLECT_ALL)  // Show all validation errors
    .build();

ConditionResult result = matcher.evaluate(formValidation, request);
if (!result.isMatched()) {
    // Return all validation errors to user
    return ResponseEntity.badRequest().body(result.getFailures());
}
```

---

## Design Philosophy

Understanding the "why" behind the design:

### 1. Functional Interfaces

**Why:** Composability and flexibility

```java
// Conditions are functions
Condition c1 = ctx -> ...;
Condition c2 = ctx -> ...;

// Easily combine
Condition combined = ConditionGroup.and(c1, c2);
```

### 2. Immutability

**Why:** Thread safety and predictability

```java
// Create once
Condition condition = new HeaderCondition(...);

// Use anywhere, anytime - always safe
executor.submit(() -> matcher.evaluate(condition, request1));
executor.submit(() -> matcher.evaluate(condition, request2));
```

### 3. Explicit Failures

**Why:** No hidden control flow, better debugging

```java
// Bad: Exceptions for control flow
try {
    validateRequest(request);
} catch (InvalidHeaderException e) {
    // Exception handling mixes errors with business logic
}

// Good: Explicit success/failure
ConditionResult result = matcher.evaluate(condition, request);
if (!result.isMatched()) {
    // Clear business logic
}
```

### 4. Separation of Concerns

**Why:** Testability and flexibility

- **Condition:** What to check (pure logic)
- **RequestContext:** Where to get data (abstraction)
- **ConditionResult:** What happened (data)
- **RequestConditionMatcher:** How to evaluate (execution)

Each component has one job and does it well.

---

## Putting It Together

Here's how all components work together:

```java
// 1. Define what to check (Condition)
Condition condition = new HeaderCondition(
    "X-Api-Key",
    "secret",
    MatchOperation.EQUALS,
    false
);

// 2. Create context from request (RequestContext)
RequestContext context = RequestContext.from(request);

// 3. Evaluate (RequestConditionMatcher)
ConditionResult result = matcher.evaluate(condition, context);

// 4. Act on result (ConditionResult + ConditionFailure)
if (result.isMatched()) {
    // Success
} else {
    // Examine failures
    result.getFailures().forEach(failure -> {
        System.out.println(failure.getConditionType());  // "Header"
        System.out.println(failure.getFieldName());      // "X-Api-Key"
        System.out.println(failure.getOperation());      // "equals"
        System.out.println(failure.getExpectedValue());  // "secret"
        System.out.println(failure.getActualValue());    // "wrong-key"
        System.out.println(failure.getMessage());        // Full message
    });
}
```

---

## Key Takeaways

1. **Conditions are functions** - Create once, reuse everywhere
2. **Results are data** - No exceptions for control flow
3. **Failures are detailed** - Always know why something didn't match
4. **Context is abstract** - Testable and flexible
5. **Operations have tradeoffs** - Choose the simplest that works
6. **Modes have purposes** - FAIL_FAST for production, COLLECT_ALL for debugging

---

## Next Steps

**Ready for practical examples?**
→ [Headers and Params](03-headers-and-params.md) - Common matching scenarios

**Want to combine conditions?**
→ [Complex Conditions](04-building-complex-conditions.md) - AND/OR logic

**Need advanced features?**
→ [JSON Matching](05-json-matching.md) - JSONPath support

---

**[← Quick Start](01-quick-start.md)** | **[Back to Index](00-index.md)** | **[Next: Headers & Params →](03-headers-and-params.md)**
