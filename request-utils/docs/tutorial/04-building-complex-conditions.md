# Building Complex Conditions

**Goal:** Master combining multiple conditions with AND/OR logic and nested groups.

This guide teaches you to build sophisticated condition trees for complex request routing and validation scenarios.

---

## Why Complex Conditions?

Real applications rarely check a single condition. You need to combine multiple checks:

**Examples:**
- "Premium users OR admin access"
- "Valid API key AND correct version AND not rate-limited"
- "Mobile user AND (beta flag OR admin)"

Request-utils provides `ConditionGroup` for composing conditions with boolean logic.

---

## The ConditionGroup Basics

`ConditionGroup` combines multiple conditions using AND or OR logic:

```java
import com.cleveloper.jufu.requestutils.condition.core.ConditionGroup;
import com.cleveloper.jufu.requestutils.condition.core.EvaluationMode;

// Using builder pattern
Condition group = ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .and(condition3)
    .mode(EvaluationMode.FAIL_FAST)  // Optional, default
    .build();

// Static factory methods
Condition andGroup = ConditionGroup.and(condition1, condition2, condition3);
Condition orGroup = ConditionGroup.or(condition1, condition2, condition3);
```

---

## AND Logic

**Semantics:** All conditions must match for the group to match.

**Use case:** Multiple required checks (authentication AND authorization AND rate limit)

### Example 1: API Gateway Validation

```java
@RestController
@RequestMapping("/api/secure")
public class SecureApiController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public SecureApiController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @PostMapping("/data")
    public ResponseEntity<?> secureEndpoint(HttpServletRequest request) {
        // All three conditions must match
        Condition securityConditions = ConditionGroup.builder()
            .and(new HeaderCondition(
                "X-Api-Key",
                "valid-key-123",
                MatchOperation.EQUALS,
                false
            ))
            .and(new HeaderCondition(
                "X-Api-Version",
                "v2",
                MatchOperation.EQUALS,
                false
            ))
            .and(new HeaderCondition(
                "Content-Type",
                "application/json",
                MatchOperation.EQUALS,
                true
            ))
            .mode(EvaluationMode.FAIL_FAST)
            .build();

        ConditionResult result = matcher.evaluate(securityConditions, request);

        if (!result.isMatched()) {
            // Log which check failed
            result.getFailures().forEach(failure ->
                System.err.println("Security check failed: " + failure.getMessage())
            );

            return ResponseEntity.status(403)
                .body(Map.of("error", "Security validation failed"));
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
```

**Testing:**
```bash
# All conditions satisfied (success)
curl -X POST http://localhost:8080/api/secure/data \
  -H "X-Api-Key: valid-key-123" \
  -H "X-Api-Version: v2" \
  -H "Content-Type: application/json"

# Missing API key (403)
curl -X POST http://localhost:8080/api/secure/data \
  -H "X-Api-Version: v2" \
  -H "Content-Type: application/json"

# Wrong version (403)
curl -X POST http://localhost:8080/api/secure/data \
  -H "X-Api-Key: valid-key-123" \
  -H "X-Api-Version: v1" \
  -H "Content-Type: application/json"
```

---

### Example 2: Form Validation

**Scenario:** Validate multiple required fields in a JSON request

```java
@PostMapping("/register")
public ResponseEntity<?> register(HttpServletRequest request) {
    // Check multiple required JSON fields
    Condition formValidation = ConditionGroup.builder()
        .and(new JsonPathCondition(
            "$.email",
            ".+@.+\\..+",
            MatchOperation.REGEX,
            false
        ))
        .and(new JsonPathCondition(
            "$.password",
            ".{8,}",  // At least 8 characters
            MatchOperation.REGEX,
            false
        ))
        .and(new JsonPathCondition(
            "$.terms",
            "true",
            MatchOperation.EQUALS,
            true
        ))
        .mode(EvaluationMode.COLLECT_ALL)  // See all validation errors
        .build();

    ConditionResult result = matcher.evaluate(formValidation, request);

    if (!result.isMatched()) {
        // Return all validation errors to user
        List<String> errors = result.getFailures().stream()
            .map(ConditionFailure::getMessage)
            .collect(Collectors.toList());

        return ResponseEntity.badRequest()
            .body(Map.of("errors", errors));
    }

    // Process registration
    return ResponseEntity.ok(Map.of("status", "registered"));
}
```

**Why COLLECT_ALL?** For form validation, users want to see all errors at once, not just the first one.

---

## OR Logic

**Semantics:** At least one condition must match for the group to match.

**Use case:** Multiple authentication methods, fallback options, alternative paths

### Example 1: Multiple Authentication Methods

```java
@GetMapping("/premium-content")
public ResponseEntity<?> premiumContent(HttpServletRequest request) {
    // Accept either JWT token OR API key OR session cookie
    Condition authConditions = ConditionGroup.builder()
        .or(new HeaderCondition(
            "Authorization",
            "Bearer ",
            MatchOperation.STARTS_WITH,
            false
        ))
        .or(new HeaderCondition(
            "X-Api-Key",
            "valid-key",
            MatchOperation.EQUALS,
            false
        ))
        .or(new QueryParamCondition(
            "session_id",
            "active-session-.*",
            MatchOperation.REGEX,
            false
        ))
        .mode(EvaluationMode.FAIL_FAST)  // Stop at first match
        .build();

    ConditionResult result = matcher.evaluate(authConditions, request);

    if (!result.isMatched()) {
        return ResponseEntity.status(401)
            .body(Map.of("error", "Authentication required"));
    }

    return ResponseEntity.ok("Premium content");
}
```

**Testing:**
```bash
# Via JWT token
curl -H "Authorization: Bearer jwt-token-123" \
  http://localhost:8080/api/premium-content

# Via API key
curl -H "X-Api-Key: valid-key" \
  http://localhost:8080/api/premium-content

# Via session
curl "http://localhost:8080/api/premium-content?session_id=active-session-abc"

# No auth (401)
curl http://localhost:8080/api/premium-content
```

---

### Example 2: Access Control

**Scenario:** Allow access to admins OR premium users

```java
@GetMapping("/special-feature")
public ResponseEntity<?> specialFeature(HttpServletRequest request) {
    Condition accessCondition = ConditionGroup.or(
        new HeaderCondition("X-User-Role", "admin", MatchOperation.EQUALS, false),
        new HeaderCondition("X-Subscription", "premium", MatchOperation.EQUALS, false)
    );

    ConditionResult result = matcher.evaluate(accessCondition, request);

    if (!result.isMatched()) {
        return ResponseEntity.status(403)
            .body(Map.of("error", "Admin or premium subscription required"));
    }

    return ResponseEntity.ok(Map.of("feature", "enabled"));
}
```

---

## Nested Groups

**Power feature:** Combine AND and OR logic in arbitrarily complex trees.

**Example structure:**
```
(condition1 AND condition2) OR (condition3 AND condition4)
```

### Example 1: Complex Access Control

**Scenario:** Allow access if:
- User is admin, OR
- User is premium AND feature flag is enabled

```java
@GetMapping("/experimental-feature")
public ResponseEntity<?> experimentalFeature(HttpServletRequest request) {
    // Admin condition
    Condition isAdmin = new HeaderCondition(
        "X-User-Role",
        "admin",
        MatchOperation.EQUALS,
        false
    );

    // Premium user condition
    Condition isPremium = new HeaderCondition(
        "X-Subscription",
        "premium",
        MatchOperation.EQUALS,
        false
    );

    // Feature flag enabled
    Condition featureEnabled = new QueryParamCondition(
        "experimental",
        "true",
        MatchOperation.EQUALS,
        true
    );

    // Nested logic: admin OR (premium AND feature flag)
    Condition accessCondition = ConditionGroup.builder()
        .or(isAdmin)
        .or(ConditionGroup.and(isPremium, featureEnabled))
        .build();

    ConditionResult result = matcher.evaluate(accessCondition, request);

    if (!result.isMatched()) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Access denied",
            "hint", "Requires admin role OR premium subscription with experimental flag"
        ));
    }

    return ResponseEntity.ok(Map.of("feature", "experimental"));
}
```

**Testing:**
```bash
# Admin (allowed)
curl -H "X-User-Role: admin" \
  http://localhost:8080/api/experimental-feature

# Premium with flag (allowed)
curl -H "X-Subscription: premium" \
  "http://localhost:8080/api/experimental-feature?experimental=true"

# Premium without flag (denied)
curl -H "X-Subscription: premium" \
  http://localhost:8080/api/experimental-feature

# Regular user with flag (denied)
curl "http://localhost:8080/api/experimental-feature?experimental=true"
```

---

### Example 2: Multi-Environment Routing

**Scenario:** Route to beta server if:
- Environment is staging OR development, AND
- User has beta flag OR is internal user

```java
@GetMapping("/api-endpoint")
public ResponseEntity<?> apiEndpoint(HttpServletRequest request) {
    // Environment conditions
    Condition isStaging = new HeaderCondition(
        "X-Environment", "staging", MatchOperation.EQUALS, true
    );
    Condition isDev = new HeaderCondition(
        "X-Environment", "development", MatchOperation.EQUALS, true
    );

    // User conditions
    Condition hasBetaFlag = new QueryParamCondition(
        "beta", "true", MatchOperation.EQUALS, true
    );
    Condition isInternal = new HeaderCondition(
        "X-User-Email", ".*@company\\.com", MatchOperation.REGEX, true
    );

    // Complex nested logic
    Condition betaRouting = ConditionGroup.builder()
        .and(
            ConditionGroup.or(isStaging, isDev),  // Environment check
            ConditionGroup.or(hasBetaFlag, isInternal)  // User check
        )
        .build();

    ConditionResult result = matcher.evaluate(betaRouting, request);

    if (result.isMatched()) {
        return serveBetaVersion();
    } else {
        return serveStableVersion();
    }
}

private ResponseEntity<?> serveBetaVersion() {
    return ResponseEntity.ok(Map.of("version", "beta"));
}

private ResponseEntity<?> serveStableVersion() {
    return ResponseEntity.ok(Map.of("version", "stable"));
}
```

---

## Builder API Deep Dive

The `ConditionGroup.builder()` provides a fluent API for complex conditions:

```java
ConditionGroup.builder()
    .and(condition1)           // Add AND condition
    .and(condition2)
    .or(condition3)            // Add OR condition
    .or(condition4)
    .mode(EvaluationMode.FAIL_FAST)  // Set evaluation mode
    .build();                  // Create immutable group
```

**Important:** Builder maintains logical consistency:

```java
// Valid: All ANDs
ConditionGroup.builder()
    .and(c1)
    .and(c2)
    .build();

// Valid: All ORs
ConditionGroup.builder()
    .or(c1)
    .or(c2)
    .build();

// Invalid: Mixing AND and OR at same level
ConditionGroup.builder()
    .and(c1)
    .or(c2)  // Compile error or runtime exception
    .build();

// Valid: Nest for mixed logic
ConditionGroup.builder()
    .and(c1)
    .and(ConditionGroup.or(c2, c3))  // Nested OR within AND
    .build();
```

---

## Evaluation Modes

Control how conditions are evaluated within a group.

### FAIL_FAST Mode (Default)

**Behavior:** Stop at first failure

**Performance:** ⚡ Fastest (short-circuit evaluation)

**Use case:** Production request filtering

```java
Condition fastGroup = ConditionGroup.builder()
    .and(expensiveCondition1)
    .and(expensiveCondition2)
    .and(expensiveCondition3)
    .mode(EvaluationMode.FAIL_FAST)  // Stop at first failure
    .build();

// If condition1 fails, conditions 2 and 3 are never evaluated
```

**Best for:**
- Authentication/authorization checks
- Rate limiting
- Request routing
- Any scenario where first failure is sufficient

---

### COLLECT_ALL Mode

**Behavior:** Evaluate all conditions, collect all failures

**Performance:** 🐌 Slower (evaluates everything)

**Use case:** Form validation, debugging

```java
Condition debugGroup = ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .and(condition3)
    .mode(EvaluationMode.COLLECT_ALL)  // Evaluate all, collect all failures
    .build();

ConditionResult result = matcher.evaluate(debugGroup, request);

// See all failures
result.getFailures().forEach(failure ->
    System.out.println("Failed: " + failure.getMessage())
);
```

**Best for:**
- Form validation (show all errors)
- API request validation
- Development/debugging
- Detailed error reporting

---

## Anti-Patterns

### 1. Overly Deep Nesting

**Bad:**
```java
// 5 levels of nesting - hard to read and debug
ConditionGroup.builder()
    .and(ConditionGroup.or(
        ConditionGroup.and(
            ConditionGroup.or(
                ConditionGroup.and(c1, c2),
                c3
            ),
            c4
        ),
        c5
    ))
    .build();
```

**Good:**
```java
// Extract intermediate groups
Condition innerAnd = ConditionGroup.and(c1, c2);
Condition innerOr = ConditionGroup.or(innerAnd, c3);
Condition middleAnd = ConditionGroup.and(innerOr, c4);
Condition outerOr = ConditionGroup.or(middleAnd, c5);
```

**Better:**
```java
// Use named constants for readability
private static final Condition IS_AUTHENTICATED =
    ConditionGroup.and(c1, c2);

private static final Condition HAS_ACCESS =
    ConditionGroup.or(IS_AUTHENTICATED, c3);

private static final Condition CAN_PROCEED =
    ConditionGroup.and(HAS_ACCESS, c4);
```

---

### 2. Recreating Conditions on Every Request

**Bad:**
```java
@GetMapping("/api")
public ResponseEntity<?> handler(HttpServletRequest request) {
    // Creating new conditions on every request
    Condition condition = ConditionGroup.builder()
        .and(new HeaderCondition(...))
        .and(new HeaderCondition(...))
        .build();

    ConditionResult result = matcher.evaluate(condition, request);
}
```

**Good:**
```java
// Create once, reuse everywhere
private static final Condition API_CONDITIONS = ConditionGroup.builder()
    .and(new HeaderCondition(...))
    .and(new HeaderCondition(...))
    .build();

@GetMapping("/api")
public ResponseEntity<?> handler(HttpServletRequest request) {
    // Reuse pre-built condition
    ConditionResult result = matcher.evaluate(API_CONDITIONS, request);
}
```

---

### 3. Using AND When OR Is Needed

**Bad:**
```java
// This requires BOTH admin AND premium (probably not what you want)
Condition wrongLogic = ConditionGroup.and(
    new HeaderCondition("X-Role", "admin", ...),
    new HeaderCondition("X-Subscription", "premium", ...)
);
```

**Good:**
```java
// Allow access if user is admin OR premium
Condition correctLogic = ConditionGroup.or(
    new HeaderCondition("X-Role", "admin", ...),
    new HeaderCondition("X-Subscription", "premium", ...)
);
```

---

### 4. Ignoring Evaluation Mode

**Bad:**
```java
// Always using COLLECT_ALL (slower)
Condition slowCondition = ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .mode(EvaluationMode.COLLECT_ALL)  // Unnecessary in production
    .build();
```

**Good:**
```java
// Use FAIL_FAST in production
Condition prodCondition = ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .mode(EvaluationMode.FAIL_FAST)  // Or omit (it's the default)
    .build();

// Use COLLECT_ALL only for validation/debugging
Condition validationCondition = ConditionGroup.builder()
    .and(formField1)
    .and(formField2)
    .mode(EvaluationMode.COLLECT_ALL)  // Makes sense here
    .build();
```

---

## Performance Tips

### 1. Order Conditions by Likelihood of Failure

```java
// Good: Put most likely failure first (fail fast)
Condition optimized = ConditionGroup.builder()
    .and(cheapAndLikelyToFail)    // Check this first
    .and(expensiveCheck)          // Skip if first fails
    .and(rarelyFailsCheck)        // Skip if either above fails
    .mode(EvaluationMode.FAIL_FAST)
    .build();
```

### 2. Cache Complex Conditions

```java
@Component
public class ConditionCache {
    private static final Map<String, Condition> CACHE = new ConcurrentHashMap<>();

    public Condition getOrCreate(String key, Supplier<Condition> supplier) {
        return CACHE.computeIfAbsent(key, k -> supplier.get());
    }
}
```

### 3. Avoid Deep Nesting

Flat structures evaluate faster than deeply nested ones:

```java
// Slower: Deep nesting
ConditionGroup.and(
    c1,
    ConditionGroup.or(
        c2,
        ConditionGroup.and(c3, c4)
    )
)

// Faster: Flat structure (if logic allows)
ConditionGroup.and(c1, c2, c3, c4)
```

### 4. Use Static Final Constants

```java
// Evaluated once at class load time
private static final Condition AUTH_CHECK = ConditionGroup.and(
    new HeaderCondition(...),
    new HeaderCondition(...)
);

// Not: Creating new instances per request
private Condition getAuthCheck() {
    return ConditionGroup.and(...);  // Bad
}
```

---

## Key Takeaways

1. **ConditionGroup** combines multiple conditions with AND/OR logic
2. **AND logic** requires all conditions to match
3. **OR logic** requires at least one condition to match
4. **Nested groups** enable arbitrarily complex logic trees
5. **FAIL_FAST** for production (performance), **COLLECT_ALL** for validation (UX)
6. **Avoid deep nesting** - extract intermediate groups for readability
7. **Reuse conditions** - create once, evaluate many times
8. **Order matters** - put likely failures first for performance

---

## Next Steps

**Ready for JSON matching?**
→ [JSON Matching](05-json-matching.md) - JSONPath and exact field matching

**Want declarative syntax?**
→ [AOP Annotations](06-aop-annotations.md) - Use `@JUFUMatchConditions`

**Need to build custom conditions?**
→ [Custom Conditions](07-custom-conditions.md) - Implement your own logic

**See real-world examples?**
→ [Complete Examples](08-complete-examples.md) - Production patterns

---

**[← Headers & Params](03-headers-and-params.md)** | **[Back to Index](00-index.md)** | **[Next: JSON Matching →](05-json-matching.md)**
