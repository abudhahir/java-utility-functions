# AOP Annotations

**Goal:** Use declarative annotations for condition matching without programmatic evaluation.

This guide covers the `@JUFUMatchConditions` annotation and related AOP features, enabling clean, annotation-driven request validation.

---

## Why AOP Annotations?

Programmatic condition evaluation works well but adds boilerplate:

```java
// Programmatic approach (works, but verbose)
@GetMapping("/api/feature")
public ResponseEntity<?> feature(HttpServletRequest request) {
    Condition condition = new HeaderCondition(...);
    ConditionResult result = matcher.evaluate(condition, request);

    if (!result.isMatched()) {
        return ResponseEntity.status(403).body("Access denied");
    }

    // Actual business logic
    return ResponseEntity.ok("Feature content");
}
```

AOP annotations move condition checking to method-level metadata:

```java
// Declarative approach (cleaner)
@GetMapping("/api/feature")
@JUFUMatchConditions(
    headers = @JUFUHeaderCondition(name = "X-User-Type", value = "premium")
)
public ResponseEntity<?> feature() {
    // Only executes if condition matches
    return ResponseEntity.ok("Feature content");
}
```

**Benefits:**
- Less boilerplate
- Cleaner business logic
- Easier to test (mock aspects)
- Centralized error handling
- Better separation of concerns

---

## Prerequisites

AOP annotations require Spring AOP. Spring Boot includes this by default, but verify:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

**Auto-configuration:** Request-utils automatically registers the AOP aspect if `@EnableAspectJAutoProxy` is detected or Spring Boot AOP is present.

---

## Basic Usage

### The @JUFUMatchConditions Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JUFUMatchConditions {
    JUFUHeaderCondition[] headers() default {};
    JUFUQueryParamCondition[] queryParams() default {};
    JUFUJsonPathCondition[] jsonPaths() default {};
    LogicMode mode() default LogicMode.AND;  // How to combine conditions
}
```

**Logic modes:**
- `LogicMode.AND` - All conditions must match (default)
- `LogicMode.OR` - At least one condition must match

---

### Example 1: Single Header Check

```java
import com.cleveloper.jufu.requestutils.condition.aop.JUFUMatchConditions;
import com.cleveloper.jufu.requestutils.condition.aop.JUFUHeaderCondition;

@RestController
@RequestMapping("/api")
public class AnnotatedController {

    @GetMapping("/premium")
    @JUFUMatchConditions(
        headers = @JUFUHeaderCondition(
            name = "X-User-Type",
            value = "premium"
        )
    )
    public ResponseEntity<?> premiumFeature() {
        // Only executes if X-User-Type header equals "premium"
        return ResponseEntity.ok("Welcome, premium user!");
    }
}
```

**Testing:**
```bash
# Success
curl -H "X-User-Type: premium" http://localhost:8080/api/premium

# Failure (throws ConditionNotMetException)
curl http://localhost:8080/api/premium
```

**What happens on failure?**
By default, throws `ConditionNotMetException`. See [Exception Handling](#exception-handling) section.

---

### Example 2: Multiple Headers (AND Logic)

```java
@PostMapping("/secure-api")
@JUFUMatchConditions(
    headers = {
        @JUFUHeaderCondition(name = "X-Api-Key", value = "secret-key"),
        @JUFUHeaderCondition(name = "X-Api-Version", value = "v2")
    }
    // Default mode = AND (all must match)
)
public ResponseEntity<?> secureApi() {
    return ResponseEntity.ok("Secure API response");
}
```

**Testing:**
```bash
# Success (both headers present)
curl -X POST http://localhost:8080/api/secure-api \
  -H "X-Api-Key: secret-key" \
  -H "X-Api-Version: v2"

# Failure (missing version header)
curl -X POST http://localhost:8080/api/secure-api \
  -H "X-Api-Key: secret-key"
```

---

### Example 3: Multiple Conditions (OR Logic)

```java
@GetMapping("/content")
@JUFUMatchConditions(
    mode = LogicMode.OR,  // At least one must match
    headers = {
        @JUFUHeaderCondition(name = "X-User-Role", value = "admin"),
        @JUFUHeaderCondition(name = "X-Subscription", value = "premium")
    }
)
public ResponseEntity<?> specialContent() {
    // Executes if user is admin OR premium
    return ResponseEntity.ok("Special content");
}
```

**Testing:**
```bash
# Success (admin)
curl -H "X-User-Role: admin" http://localhost:8080/api/content

# Success (premium)
curl -H "X-Subscription: premium" http://localhost:8080/api/content

# Success (both)
curl -H "X-User-Role: admin" -H "X-Subscription: premium" \
  http://localhost:8080/api/content

# Failure (neither)
curl http://localhost:8080/api/content
```

---

## Query Parameters

Use `@JUFUQueryParamCondition` for query parameter matching:

```java
@GetMapping("/beta-feature")
@JUFUMatchConditions(
    queryParams = @JUFUQueryParamCondition(
        name = "beta",
        value = "true",
        ignoreCase = true
    )
)
public ResponseEntity<?> betaFeature() {
    return ResponseEntity.ok("Beta feature enabled");
}
```

**Testing:**
```bash
# Success
curl "http://localhost:8080/api/beta-feature?beta=true"
curl "http://localhost:8080/api/beta-feature?beta=TRUE"  # ignoreCase = true

# Failure
curl "http://localhost:8080/api/beta-feature"
```

---

## JSON Path Matching

Use `@JUFUJsonPathCondition` for JSON payload validation:

```java
@PostMapping("/tenant-api")
@JUFUMatchConditions(
    jsonPaths = @JUFUJsonPathCondition(
        path = "$.tenant.id",
        value = "acme-corp"
    )
)
public ResponseEntity<?> tenantApi() {
    // Only executes if request body has tenant.id = "acme-corp"
    return ResponseEntity.ok("Tenant API response");
}
```

**Testing:**
```bash
# Success
curl -X POST http://localhost:8080/api/tenant-api \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"id": "acme-corp"}, "data": {}}'

# Failure
curl -X POST http://localhost:8080/api/tenant-api \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"id": "other-corp"}, "data": {}}'
```

---

## Combining Different Condition Types

Mix headers, query parameters, and JSON paths:

```java
@PostMapping("/complex")
@JUFUMatchConditions(
    mode = LogicMode.AND,  // All must match
    headers = @JUFUHeaderCondition(
        name = "X-Api-Key",
        value = "valid-key"
    ),
    queryParams = @JUFUQueryParamCondition(
        name = "version",
        value = "v2"
    ),
    jsonPaths = @JUFUJsonPathCondition(
        path = "$.tenant.plan",
        value = "premium"
    )
)
public ResponseEntity<?> complexEndpoint() {
    // Executes only if:
    // - Header X-Api-Key = "valid-key" AND
    // - Query param version = "v2" AND
    // - JSON field $.tenant.plan = "premium"
    return ResponseEntity.ok("All conditions matched");
}
```

**Testing:**
```bash
# Success (all conditions met)
curl -X POST "http://localhost:8080/api/complex?version=v2" \
  -H "X-Api-Key: valid-key" \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"plan": "premium"}}'

# Failure (missing query param)
curl -X POST http://localhost:8080/api/complex \
  -H "X-Api-Key: valid-key" \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"plan": "premium"}}'
```

---

## Match Operations

Specify match operation for advanced matching:

```java
@GetMapping("/mobile")
@JUFUMatchConditions(
    headers = @JUFUHeaderCondition(
        name = "User-Agent",
        value = ".*Mobile.*",
        operation = MatchOperation.REGEX,
        ignoreCase = true
    )
)
public ResponseEntity<?> mobileContent() {
    return ResponseEntity.ok("Mobile-optimized content");
}
```

**Available operations:**
- `MatchOperation.EQUALS` (default)
- `MatchOperation.CONTAINS`
- `MatchOperation.STARTS_WITH`
- `MatchOperation.ENDS_WITH`
- `MatchOperation.REGEX`

---

## Exception Handling

When conditions don't match, `ConditionNotMetException` is thrown. Handle it globally:

```java
import com.cleveloper.jufu.requestutils.condition.exceptions.ConditionNotMetException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<?> handleConditionNotMet(ConditionNotMetException ex) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Access denied",
            "message", ex.getMessage(),
            "failures", ex.getResult().getFailures().stream()
                .map(failure -> Map.of(
                    "field", failure.getFieldName(),
                    "expected", failure.getExpectedValue(),
                    "actual", failure.getActualValue()
                ))
                .collect(Collectors.toList())
        ));
    }
}
```

**Response example:**
```json
{
  "error": "Access denied",
  "message": "Request conditions not met",
  "failures": [
    {
      "field": "X-Api-Key",
      "expected": "valid-key",
      "actual": "[not present]"
    }
  ]
}
```

---

## Request Extraction

Access the validated request in your method:

```java
@GetMapping("/user-info")
@JUFUMatchConditions(
    headers = @JUFUHeaderCondition(name = "X-User-Id", value = ".*", operation = MatchOperation.REGEX)
)
public ResponseEntity<?> userInfo(HttpServletRequest request) {
    // Request is available and validated
    String userId = request.getHeader("X-User-Id");
    return ResponseEntity.ok(Map.of("userId", userId));
}
```

Or use Spring's method parameter injection:

```java
@GetMapping("/user-data")
@JUFUMatchConditions(
    headers = @JUFUHeaderCondition(name = "X-User-Id", value = ".*", operation = MatchOperation.REGEX)
)
public ResponseEntity<?> userData(@RequestHeader("X-User-Id") String userId) {
    // Spring extracts header after validation
    return ResponseEntity.ok(Map.of("userId", userId));
}
```

---

## Programmatic vs Declarative

**When to use programmatic approach:**

```java
// Use programmatic when:
// 1. Conditional logic is complex
// 2. Conditions are determined at runtime
// 3. Need fine-grained control over failure handling
// 4. Multiple code paths based on different conditions

@GetMapping("/dynamic")
public ResponseEntity<?> dynamicEndpoint(HttpServletRequest request) {
    Condition condition1 = ...;
    Condition condition2 = ...;

    ConditionResult result1 = matcher.evaluate(condition1, request);
    if (result1.isMatched()) {
        return handlePath1();
    }

    ConditionResult result2 = matcher.evaluate(condition2, request);
    if (result2.isMatched()) {
        return handlePath2();
    }

    return handleDefault();
}
```

**When to use declarative approach:**

```java
// Use declarative when:
// 1. Conditions are static and known at compile time
// 2. Simple pass/fail validation
// 3. Want cleaner code
// 4. Consistent error handling across endpoints

@GetMapping("/static")
@JUFUMatchConditions(
    headers = @JUFUHeaderCondition(name = "X-Api-Key", value = "secret")
)
public ResponseEntity<?> staticEndpoint() {
    // Clean business logic without validation boilerplate
    return ResponseEntity.ok("Success");
}
```

---

## When NOT to Use AOP

### 1. Complex Conditional Logic

**Bad:**
```java
// Can't express "if admin OR (premium AND beta-flag)" with annotations
@JUFUMatchConditions(...)  // Limited expressiveness
```

**Good:**
```java
// Use programmatic approach for complex logic
Condition complex = ConditionGroup.builder()
    .or(isAdmin)
    .or(ConditionGroup.and(isPremium, hasBetaFlag))
    .build();
```

### 2. Runtime-Determined Conditions

**Bad:**
```java
// Can't change annotation values at runtime
@JUFUMatchConditions(
    headers = @JUFUHeaderCondition(name = "X-Tenant", value = ???)  // Unknown at compile time
)
```

**Good:**
```java
// Determine condition at runtime
String tenantId = getTenantIdFromConfig();
Condition condition = new HeaderCondition("X-Tenant", tenantId, ...);
```

### 3. Multiple Return Paths

**Bad:**
```java
// Can't route to different handlers based on conditions
@JUFUMatchConditions(...)
public ResponseEntity<?> endpoint() {
    // All or nothing - can't differentiate which condition matched
}
```

**Good:**
```java
// Route based on which condition matches
if (matcher.evaluate(conditionA, request).isMatched()) {
    return handleA();
} else if (matcher.evaluate(conditionB, request).isMatched()) {
    return handleB();
} else {
    return handleDefault();
}
```

### 4. Need Access to ConditionResult

**Bad:**
```java
// AOP throws exception - can't access ConditionResult
@JUFUMatchConditions(...)
public ResponseEntity<?> endpoint() {
    // Can't see why condition failed
}
```

**Good:**
```java
// Access detailed failure information
ConditionResult result = matcher.evaluate(condition, request);
if (!result.isMatched()) {
    result.getFailures().forEach(failure ->
        log.warn("Failed: {}", failure.getMessage())
    );
}
```

---

## Complete Endpoint Example

Real-world endpoint combining all concepts:

```java
@RestController
@RequestMapping("/api/saas")
public class SaaSController {

    /**
     * Premium feature endpoint with multi-factor validation.
     *
     * Requires:
     * - Valid API key in header
     * - Premium subscription in JSON payload
     * - Version 2 API
     *
     * @return Premium feature response
     * @throws ConditionNotMetException if conditions not met (handled by GlobalExceptionHandler)
     */
    @PostMapping("/premium-feature")
    @JUFUMatchConditions(
        mode = LogicMode.AND,
        headers = {
            @JUFUHeaderCondition(
                name = "X-Api-Key",
                value = "sk-prod-.*",
                operation = MatchOperation.REGEX,
                ignoreCase = false
            ),
            @JUFUHeaderCondition(
                name = "X-Api-Version",
                value = "v2",
                operation = MatchOperation.EQUALS,
                ignoreCase = false
            )
        },
        jsonPaths = @JUFUJsonPathCondition(
            path = "$.subscription.tier",
            value = "premium",
            operation = MatchOperation.EQUALS,
            ignoreCase = true
        )
    )
    public ResponseEntity<?> premiumFeature(@RequestBody Map<String, Object> payload) {
        // All validations passed - implement business logic
        String featureData = processPremiumFeature(payload);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "data", featureData,
            "tier", "premium"
        ));
    }

    private String processPremiumFeature(Map<String, Object> payload) {
        // Business logic
        return "Premium feature result";
    }
}
```

**Global exception handler:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<Map<String, Object>> handleConditionNotMet(
            ConditionNotMetException ex,
            HttpServletRequest request
    ) {
        // Log the failure
        log.warn("Condition not met for {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );

        // Build detailed error response
        List<Map<String, String>> failures = ex.getResult().getFailures().stream()
            .map(failure -> Map.of(
                "type", failure.getConditionType(),
                "field", failure.getFieldName(),
                "expected", failure.getExpectedValue(),
                "actual", failure.getActualValue(),
                "message", failure.getMessage()
            ))
            .collect(Collectors.toList());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Access denied");
        errorResponse.put("message", "Request validation failed");
        errorResponse.put("failures", failures);
        errorResponse.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
}
```

**Testing:**
```bash
# Success
curl -X POST http://localhost:8080/api/saas/premium-feature \
  -H "X-Api-Key: sk-prod-abc123" \
  -H "X-Api-Version: v2" \
  -H "Content-Type: application/json" \
  -d '{"subscription": {"tier": "premium"}}'

# Failure (wrong version)
curl -X POST http://localhost:8080/api/saas/premium-feature \
  -H "X-Api-Key: sk-prod-abc123" \
  -H "X-Api-Version: v1" \
  -H "Content-Type: application/json" \
  -d '{"subscription": {"tier": "premium"}}'
# Response: 403 with detailed failure information
```

---

## Key Takeaways

1. **@JUFUMatchConditions** provides declarative condition matching
2. **LogicMode.AND** requires all conditions, **LogicMode.OR** requires at least one
3. **ConditionNotMetException** is thrown on failure - handle globally
4. **Mix condition types** (headers, query params, JSON) in single annotation
5. **Use programmatic** for complex logic, **use declarative** for simple validation
6. **Exception handler** centralizes error responses
7. **Request available** in method parameters after validation

---

## Next Steps

**Need custom validation logic?**
→ [Custom Conditions](07-custom-conditions.md) - Implement your own condition classes

**See complete working examples?**
→ [Complete Examples](08-complete-examples.md) - Real-world applications with AOP

**Having issues?**
→ [Troubleshooting](09-troubleshooting.md) - Debug AOP problems

---

**[← JSON Matching](05-json-matching.md)** | **[Back to Index](00-index.md)** | **[Next: Custom Conditions →](07-custom-conditions.md)**
