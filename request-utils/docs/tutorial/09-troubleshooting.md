# Troubleshooting

**Goal:** Diagnose and fix common issues with request condition matching.

This guide is organized by symptom. Find your problem, follow the diagnostics, apply the solution.

---

## Symptom: Condition Not Matching

### Problem: Header condition always fails despite header being present

**Symptom:**
```java
Condition condition = new HeaderCondition("content-type", "application/json", ...);
// Always fails even when Content-Type header is sent
```

**Diagnosis:**

1. Check header name casing:
```java
// Log all headers
request.getHeaderNames().asIterator().forEachRemaining(name ->
    System.out.println(name + ": " + request.getHeader(name))
);
```

2. Enable condition logging:
```java
ConditionResult result = matcher.evaluate(condition, request);
result.getFailures().forEach(failure ->
    System.out.println("Failure: " + failure.getMessage())
);
```

**Solutions:**

**Solution 1: Use standard header name casing**
```java
// Correct
new HeaderCondition("Content-Type", "application/json", ...)

// Wrong
new HeaderCondition("content-type", "application/json", ...)  // May not match
```

**Solution 2: Check actual header value**
```java
String actualValue = request.getHeader("Content-Type");
System.out.println("Actual Content-Type: '" + actualValue + "'");

// Common issue: extra whitespace or charset
// Actual: "application/json; charset=UTF-8"
// Expected: "application/json"

// Fix: Use CONTAINS instead of EQUALS
new HeaderCondition(
    "Content-Type",
    "application/json",
    MatchOperation.CONTAINS,  // More flexible
    true  // Case insensitive
)
```

**Solution 3: Case sensitivity**
```java
// Default is case-sensitive (false = case sensitive)
new HeaderCondition("X-Api-Key", "ABC123", MatchOperation.EQUALS, false)
// Only matches "ABC123", not "abc123"

// For case-insensitive matching
new HeaderCondition("X-Api-Key", "ABC123", MatchOperation.EQUALS, true)
// Matches "ABC123", "abc123", "Abc123", etc.
```

---

### Problem: Query parameter not matching

**Symptom:**
```bash
curl "http://localhost:8080/api?filter=status:active"
# Condition fails even though parameter is present
```

**Diagnosis:**

```java
String paramValue = request.getParameter("filter");
System.out.println("Actual parameter value: '" + paramValue + "'");
```

**Solutions:**

**Solution 1: URL encoding**
```bash
# Special characters must be URL-encoded
# Wrong
curl "http://localhost:8080/api?filter=status:active"

# Correct
curl "http://localhost:8080/api?filter=status%3Aactive"
```

**Solution 2: Use more flexible matching**
```java
// Instead of exact match
new QueryParamCondition("filter", "status:active", MatchOperation.EQUALS, false)

// Use CONTAINS for partial match
new QueryParamCondition("filter", "active", MatchOperation.CONTAINS, true)
```

**Solution 3: Check for null/empty**
```java
String paramValue = request.getParameter("filter");
if (paramValue == null) {
    // Parameter not present
} else if (paramValue.isEmpty()) {
    // Parameter present but empty: ?filter=
}
```

---

### Problem: JSON path not matching

**Symptom:**
```java
// JSON: {"user": {"email": "test@example.com"}}
// Condition: $.user.email
// Result: Not matched or exception
```

**Diagnosis:**

```java
// Log the parsed JSON
RequestContext context = RequestContext.from(request);
Object json = context.getJsonBody();
System.out.println("Parsed JSON: " + json);

// Test JSONPath manually
import com.jayway.jsonpath.JsonPath;
String jsonString = /* request body */;
Object value = JsonPath.read(jsonString, "$.user.email");
System.out.println("Extracted value: " + value);
```

**Solutions:**

**Solution 1: Verify Content-Type header**
```bash
# Must include Content-Type header
curl -X POST http://localhost:8080/api \
  -H "Content-Type: application/json" \  # Required!
  -d '{"user": {"email": "test@example.com"}}'
```

**Solution 2: Check JSONPath syntax**
```java
// Correct
"$.user.email"          // Root → user → email
"$.items[0]"            // First array element
"$.users[*].name"       // All user names

// Wrong
"user.email"            // Missing $ root
"$.user.email."         // Trailing dot
"$['user']['email']"    // Use dot notation for simple paths
```

**Solution 3: Handle missing fields**
```java
try {
    ConditionResult result = matcher.evaluate(condition, request);
} catch (PathNotFoundException e) {
    // Field doesn't exist in JSON
    return ResponseEntity.badRequest().body(
        "Required field missing: " + e.getMessage()
    );
}
```

**Solution 4: Validate JSON structure first**
```java
// Check if Content-Type is JSON
Condition isJson = new HeaderCondition(
    "Content-Type",
    "application/json",
    MatchOperation.CONTAINS,
    true
);

if (!matcher.evaluate(isJson, request).isMatched()) {
    return ResponseEntity.status(415)
        .body("Content-Type must be application/json");
}

// Then check JSON fields
Condition jsonCondition = new JsonPathCondition(...);
```

---

## Symptom: NullPointerException or Missing Request

### Problem: NPE in condition evaluation

**Symptom:**
```
java.lang.NullPointerException: Cannot invoke "HttpServletRequest.getHeader(String)" on null
```

**Diagnosis:**

This happens when `RequestContext` is created from a null request:

```java
HttpServletRequest request = null;  // Problem!
RequestContext context = RequestContext.from(request);
```

**Solutions:**

**Solution 1: Ensure request is injected**
```java
// Wrong: Request parameter not injected
@GetMapping("/api")
public ResponseEntity<?> endpoint() {
    HttpServletRequest request = null;  // No request!
    ConditionResult result = matcher.evaluate(condition, request);
}

// Correct: Inject request parameter
@GetMapping("/api")
public ResponseEntity<?> endpoint(HttpServletRequest request) {
    ConditionResult result = matcher.evaluate(condition, request);
}
```

**Solution 2: Check AOP configuration**

With AOP annotations, ensure aspect can access request:

```java
// Aspect needs access to HttpServletRequest
@Around("@annotation(JUFUMatchConditions)")
public Object checkConditions(ProceedingJoinPoint pjp) throws Throwable {
    // Extract request from method parameters
    HttpServletRequest request = null;
    for (Object arg : pjp.getArgs()) {
        if (arg instanceof HttpServletRequest) {
            request = (HttpServletRequest) arg;
            break;
        }
    }

    if (request == null) {
        throw new IllegalStateException(
            "HttpServletRequest not found in method parameters. " +
            "Add HttpServletRequest parameter to method."
        );
    }

    // Evaluate conditions...
}
```

**Solution 3: Add request parameter to annotated methods**
```java
// Wrong: No request parameter
@GetMapping("/api")
@JUFUMatchConditions(...)
public ResponseEntity<?> endpoint() {
    // Aspect can't find request!
}

// Correct: Include request parameter
@GetMapping("/api")
@JUFUMatchConditions(...)
public ResponseEntity<?> endpoint(HttpServletRequest request) {
    // Aspect can now access request
}
```

---

## Symptom: JSON Issues

### Problem: JSONPath dependency not found

**Symptom:**
```
java.lang.ClassNotFoundException: com.jayway.jsonpath.JsonPath
```

**Solution:**

Add JSONPath dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.9.0</version>
</dependency>
```

Then rebuild:
```bash
./mvnw clean install
```

---

### Problem: JSON parsing fails

**Symptom:**
```
com.fasterxml.jackson.core.JsonParseException: Unexpected character
```

**Diagnosis:**

```bash
# Check request body
curl -X POST http://localhost:8080/api \
  -H "Content-Type: application/json" \
  -d '{"user": {"email": "test@example.com"}}'  # Valid JSON

# vs

curl -X POST http://localhost:8080/api \
  -H "Content-Type: application/json" \
  -d '{user: {email: "test"}'  # Invalid JSON (missing quotes)
```

**Solutions:**

**Solution 1: Validate JSON syntax**

Use online validator: [jsonlint.com](https://jsonlint.com)

**Solution 2: Handle parse errors**
```java
@ExceptionHandler(JsonParseException.class)
public ResponseEntity<?> handleJsonParseError(JsonParseException ex) {
    return ResponseEntity.badRequest().body(Map.of(
        "error", "Invalid JSON",
        "message", ex.getMessage()
    ));
}
```

---

### Problem: JSON field value is wrong type

**Symptom:**
```java
// JSON: {"count": 123}
// Condition: $.count equals "123"
// Result: Not matched (number vs string)
```

**Solution:**

JSONPath extracts values with their JSON types. Convert as needed:

```java
// For number comparison, use regex or convert
new JsonPathCondition(
    "$.count",
    "123",  // String comparison works if JSON has "123" (string)
    MatchOperation.EQUALS,
    false
)

// Or match number pattern
new JsonPathCondition(
    "$.count",
    "\\d+",  // Match any number
    MatchOperation.REGEX,
    false
)
```

---

## Symptom: AOP Not Triggering

### Problem: @JUFUMatchConditions annotation ignored

**Symptom:**
```java
@GetMapping("/api")
@JUFUMatchConditions(...)
public ResponseEntity<?> endpoint() {
    // Method executes even when conditions don't match
}
```

**Diagnosis:**

1. Check if AOP is enabled:
```java
// In your main application class or config
@EnableAspectJAutoProxy
@SpringBootApplication
public class Application {
    // ...
}
```

2. Verify Spring AOP dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

3. Check aspect is registered:
```java
// Enable debug logging
logging.level.org.springframework.aop=DEBUG
```

**Solutions:**

**Solution 1: Enable AOP**
```java
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**Solution 2: Verify aspect bean**
```java
// Ensure RequestConditionAspect is a Spring bean
@Aspect
@Component  // Must be a component!
public class RequestConditionAspect {
    // ...
}
```

**Solution 3: Check method visibility**
```java
// AOP only works on public methods
// Wrong
@JUFUMatchConditions(...)
private ResponseEntity<?> endpoint() { }  // Private - AOP won't work

// Correct
@JUFUMatchConditions(...)
public ResponseEntity<?> endpoint() { }  // Public - AOP works
```

**Solution 4: Avoid self-invocation**
```java
// Wrong: Self-invocation bypasses AOP
@RestController
public class MyController {
    @JUFUMatchConditions(...)
    public void methodA() { }

    public void methodB() {
        this.methodA();  // Direct call bypasses proxy!
    }
}

// Correct: Call through injected bean
@RestController
public class MyController {
    @Autowired
    private MyController self;

    @JUFUMatchConditions(...)
    public void methodA() { }

    public void methodB() {
        self.methodA();  // Goes through proxy
    }
}
```

---

## Symptom: Performance Issues

### Problem: Slow request processing

**Diagnosis:**

1. Enable timing logs:
```java
long start = System.currentTimeMillis();
ConditionResult result = matcher.evaluate(condition, request);
long duration = System.currentTimeMillis() - start;
System.out.println("Evaluation took: " + duration + "ms");
```

2. Check evaluation mode:
```java
// COLLECT_ALL evaluates all conditions (slower)
ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .mode(EvaluationMode.COLLECT_ALL)  // Slow!
    .build();

// FAIL_FAST stops at first failure (faster)
ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .mode(EvaluationMode.FAIL_FAST)  // Fast!
    .build();
```

**Solutions:**

**Solution 1: Use FAIL_FAST mode**
```java
// Production: Use FAIL_FAST
Condition prodCondition = ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .mode(EvaluationMode.FAIL_FAST)  // Default, explicit for clarity
    .build();

// Development/Validation: Use COLLECT_ALL
Condition validationCondition = ConditionGroup.builder()
    .and(formField1)
    .and(formField2)
    .mode(EvaluationMode.COLLECT_ALL)  // See all errors
    .build();
```

**Solution 2: Order conditions by failure likelihood**
```java
// Put most likely failure first
Condition optimized = ConditionGroup.builder()
    .and(cheapConditionLikelyToFail)     // Check this first
    .and(expensiveCondition)             // Skip if first fails
    .and(rarelyFailsCondition)           // Skip if either above fails
    .mode(EvaluationMode.FAIL_FAST)
    .build();
```

**Solution 3: Cache condition instances**
```java
// Bad: Creating new conditions per request
@GetMapping("/api")
public ResponseEntity<?> endpoint(HttpServletRequest request) {
    Condition condition = new HeaderCondition(...);  // New instance every time
    ConditionResult result = matcher.evaluate(condition, request);
}

// Good: Reuse condition instances
private static final Condition HEADER_CHECK = new HeaderCondition(...);

@GetMapping("/api")
public ResponseEntity<?> endpoint(HttpServletRequest request) {
    ConditionResult result = matcher.evaluate(HEADER_CHECK, request);  // Reuse
}
```

**Solution 4: Optimize JSON parsing**
```java
// Request body is parsed once and cached in RequestContext
// Don't create multiple contexts from same request

// Bad
RequestContext context1 = RequestContext.from(request);  // Parse JSON
RequestContext context2 = RequestContext.from(request);  // Parse JSON again!

// Good
RequestContext context = RequestContext.from(request);  // Parse once
condition1.evaluate(context);  // Reuse parsed JSON
condition2.evaluate(context);  // Reuse parsed JSON
```

**Solution 5: Avoid regex for simple matches**
```java
// Slow: Regex for simple comparison
new HeaderCondition("X-Api-Key", "secret", MatchOperation.REGEX, false)

// Fast: Direct equality
new HeaderCondition("X-Api-Key", "secret", MatchOperation.EQUALS, false)

// Match operation performance: EQUALS > STARTS_WITH/ENDS_WITH > CONTAINS > REGEX
```

---

## Symptom: ConditionNotMetException Handling

### Problem: Exception not caught by global handler

**Symptom:**
```java
@ExceptionHandler(ConditionNotMetException.class)
public ResponseEntity<?> handleCondition(ConditionNotMetException ex) {
    // Never called!
}
```

**Solutions:**

**Solution 1: Use @RestControllerAdvice**
```java
// Wrong: Missing @RestControllerAdvice
@Component
public class MyExceptionHandler {
    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<?> handle(ConditionNotMetException ex) { }
}

// Correct: Add @RestControllerAdvice
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<?> handle(ConditionNotMetException ex) { }
}
```

**Solution 2: Check exception type**
```java
import com.cleveloper.jufu.requestutils.condition.exceptions.ConditionNotMetException;

// Make sure you're importing the correct exception class
@ExceptionHandler(ConditionNotMetException.class)
public ResponseEntity<?> handle(ConditionNotMetException ex) {
    // Access condition failures
    List<ConditionFailure> failures = ex.getResult().getFailures();
    // ...
}
```

**Solution 3: Catch in specific controller**
```java
@RestController
public class MyController {

    @GetMapping("/api")
    @JUFUMatchConditions(...)
    public ResponseEntity<?> endpoint() { }

    // Controller-specific handler (higher priority than global)
    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<?> handleLocal(ConditionNotMetException ex) {
        return ResponseEntity.status(403).body("Custom local error");
    }
}
```

---

### Problem: Want to customize error response

**Solution:**

```java
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<ErrorResponse> handle(
            ConditionNotMetException ex,
            HttpServletRequest request
    ) {
        // Build custom error response
        ErrorResponse error = new ErrorResponse(
            "CONDITION_NOT_MET",
            "Request validation failed",
            request.getRequestURI(),
            Instant.now(),
            ex.getResult().getFailures().stream()
                .map(f -> new FailureDetail(
                    f.getConditionType(),
                    f.getFieldName(),
                    f.getExpectedValue(),
                    f.getActualValue()
                ))
                .collect(Collectors.toList())
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp,
        List<FailureDetail> failures
    ) {}

    record FailureDetail(
        String type,
        String field,
        String expected,
        String actual
    ) {}
}
```

---

## FAQ

### Q: Can I use condition matching outside of web requests?

**A:** Yes! Create `RequestContext` manually:

```java
// Create custom context
RequestContext context = new RequestContext() {
    @Override
    public String getHeader(String name) {
        return myHeaders.get(name);
    }

    @Override
    public String getQueryParam(String name) {
        return myParams.get(name);
    }

    @Override
    public Object getJsonBody() {
        return myJsonObject;
    }
};

// Evaluate condition
ConditionResult result = condition.evaluate(context);
```

---

### Q: Can I chain conditions dynamically?

**A:** Yes:

```java
ConditionGroup.Builder builder = ConditionGroup.builder();

if (needsAuth) {
    builder.and(authCondition);
}

if (needsVersion) {
    builder.and(versionCondition);
}

Condition dynamic = builder.build();
```

---

### Q: How do I debug which condition failed in a complex group?

**A:** Use COLLECT_ALL mode:

```java
Condition complex = ConditionGroup.builder()
    .and(condition1)
    .and(condition2)
    .and(condition3)
    .mode(EvaluationMode.COLLECT_ALL)  // Evaluate all
    .build();

ConditionResult result = matcher.evaluate(complex, request);

// See all failures
result.getFailures().forEach(failure ->
    System.out.println(
        failure.getConditionType() + " - " +
        failure.getFieldName() + ": " +
        failure.getMessage()
    )
);
```

---

### Q: Can I use conditions in scheduled tasks or async methods?

**A:** Not directly (no HttpServletRequest), but you can create a context:

```java
@Scheduled(fixedRate = 60000)
public void scheduledTask() {
    // No HttpServletRequest available
    // Create custom context with needed data
    RequestContext context = createCustomContext();
    ConditionResult result = condition.evaluate(context);
}
```

---

### Q: How do I test controllers with @JUFUMatchConditions?

**A:** Use MockMvc:

```java
@Test
void testCondition() throws Exception {
    mockMvc.perform(get("/api")
            .header("X-Api-Key", "valid-key"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api"))  // Missing header
        .andExpect(status().isForbidden());
}
```

---

### Q: Can I log all condition evaluations?

**A:** Yes, wrap matcher:

```java
@Component
public class LoggingRequestConditionMatcher {

    private final RequestConditionMatcher delegate;

    public LoggingRequestConditionMatcher(RequestConditionMatcher matcher) {
        this.delegate = matcher;
    }

    public ConditionResult evaluate(Condition condition, HttpServletRequest request) {
        long start = System.currentTimeMillis();
        ConditionResult result = delegate.evaluate(condition, request);
        long duration = System.currentTimeMillis() - start;

        System.out.println(String.format(
            "Condition: %s | Matched: %s | Duration: %dms",
            condition.getClass().getSimpleName(),
            result.isMatched(),
            duration
        ));

        return result;
    }
}
```

---

## Still Having Issues?

1. **Check the examples:** [Complete Examples](08-complete-examples.md) has working code
2. **Review fundamentals:** [Core Concepts](02-core-concepts.md) explains architecture
3. **Search GitHub issues:** [GitHub Issues](https://github.com/abudhahir/java-utility-functions/issues)
4. **Ask the community:** [GitHub Discussions](https://github.com/abudhahir/java-utility-functions/discussions)

---

## Common Error Messages Reference

| Error | Likely Cause | Solution |
|-------|-------------|----------|
| `ClassNotFoundException: JsonPath` | Missing JSONPath dependency | Add json-path to pom.xml |
| `NullPointerException` in evaluate | Request is null | Inject HttpServletRequest parameter |
| `ConditionNotMetException` not caught | Missing @RestControllerAdvice | Add global exception handler |
| AOP annotation ignored | AOP not enabled | Add @EnableAspectJAutoProxy |
| Header always fails | Wrong header name casing | Use standard casing (e.g., "Content-Type") |
| JSON path not found | Missing field or wrong path | Check path syntax and JSON structure |
| Rate limit not working | Condition created per request | Use static final constant |
| Slow performance | Using COLLECT_ALL or regex | Switch to FAIL_FAST and simpler operations |

---

**[← Complete Examples](08-complete-examples.md)** | **[Back to Index](00-index.md)**
