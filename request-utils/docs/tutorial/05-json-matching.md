# JSON Matching

**Goal:** Match and validate JSON request payloads using JSONPath expressions.

This guide covers extracting and matching JSON fields from POST/PUT request bodies, enabling sophisticated payload validation.

---

## Prerequisites

JSON matching requires the JSONPath library. Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.9.0</version>
</dependency>
```

**Note:** If you already have Spring Boot Web, you likely have Jackson for JSON parsing. JSONPath adds query capabilities on top.

---

## Why JSON Matching?

REST APIs often need to route or validate requests based on payload content:

**Examples:**
- Multi-tenant routing: Route by `$.tenant.id` in request body
- Webhook validation: Check `$.event.type` equals "user.created"
- Conditional processing: Different logic for `$.order.priority` values
- Schema validation: Ensure required fields exist and match patterns

Traditional approaches require parsing JSON manually. JSON matching does this declaratively.

---

## JSONPath Basics

JSONPath is a query language for JSON, similar to XPath for XML.

### Common Expressions

| Expression | Meaning | Example Value |
|------------|---------|---------------|
| `$.field` | Root field | `{"field": "value"}` → "value" |
| `$.user.email` | Nested field | `{"user": {"email": "a@b.com"}}` → "a@b.com" |
| `$.items[0]` | Array element | `{"items": ["a", "b"]}` → "a" |
| `$.items[*]` | All array elements | `{"items": [1, 2, 3]}` → [1, 2, 3] |
| `$..email` | Recursive descent | Any `email` field at any depth |
| `$.user['name']` | Bracket notation | Same as `$.user.name` |

**Full reference:** [JSONPath Syntax](https://goessner.net/articles/JsonPath/)

### Example JSON

Throughout this guide, we'll use this sample payload:

```json
{
  "tenant": {
    "id": "acme-corp",
    "plan": "premium"
  },
  "user": {
    "email": "admin@acme.com",
    "role": "admin"
  },
  "event": {
    "type": "order.created",
    "priority": "high"
  },
  "metadata": {
    "source": "mobile-app",
    "version": "2.1"
  }
}
```

---

## JsonPathCondition

Extracts a JSON value using JSONPath and matches it against a pattern.

**Constructor:**
```java
public JsonPathCondition(
    String jsonPath,              // JSONPath expression
    String expectedValue,         // Expected value or pattern
    MatchOperation operation,     // How to match
    boolean ignoreCase            // Case sensitivity
)
```

### Example 1: Tenant Routing

**Scenario:** Route requests to different services based on tenant ID

```java
import com.cleveloper.jufu.requestutils.condition.matchers.JsonPathCondition;

@RestController
@RequestMapping("/api")
public class TenantController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public TenantController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(HttpServletRequest request) {
        // Check if tenant is "acme-corp"
        Condition acmeTenant = new JsonPathCondition(
            "$.tenant.id",
            "acme-corp",
            MatchOperation.EQUALS,
            false
        );

        ConditionResult result = matcher.evaluate(acmeTenant, request);

        if (result.isMatched()) {
            return handleAcmeRequest();
        } else {
            return handleGenericRequest();
        }
    }

    private ResponseEntity<?> handleAcmeRequest() {
        return ResponseEntity.ok(Map.of("service", "acme-premium"));
    }

    private ResponseEntity<?> handleGenericRequest() {
        return ResponseEntity.ok(Map.of("service", "standard"));
    }
}
```

**Testing:**
```bash
curl -X POST http://localhost:8080/api/process \
  -H "Content-Type: application/json" \
  -d '{
    "tenant": {"id": "acme-corp"},
    "data": "test"
  }'
# Response: {"service": "acme-premium"}

curl -X POST http://localhost:8080/api/process \
  -H "Content-Type: application/json" \
  -d '{
    "tenant": {"id": "other-tenant"},
    "data": "test"
  }'
# Response: {"service": "standard"}
```

---

### Example 2: Webhook Event Filtering

**Scenario:** Process only specific webhook event types

```java
@PostMapping("/webhooks")
public ResponseEntity<?> handleWebhook(HttpServletRequest request) {
    // Only process "order.created" events
    Condition orderCreated = new JsonPathCondition(
        "$.event.type",
        "order.created",
        MatchOperation.EQUALS,
        false
    );

    ConditionResult result = matcher.evaluate(orderCreated, request);

    if (!result.isMatched()) {
        return ResponseEntity.ok(Map.of(
            "status", "ignored",
            "reason", "Event type not supported"
        ));
    }

    // Process order creation
    return ResponseEntity.ok(Map.of("status", "processed"));
}
```

**Testing:**
```bash
# Processed
curl -X POST http://localhost:8080/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{"event": {"type": "order.created"}, "data": {}}'

# Ignored
curl -X POST http://localhost:8080/api/webhooks \
  -H "Content-Type: application/json" \
  -d '{"event": {"type": "user.updated"}, "data": {}}'
```

---

### Example 3: Pattern Matching with Regex

**Scenario:** Validate email format in JSON payload

```java
@PostMapping("/register")
public ResponseEntity<?> register(HttpServletRequest request) {
    // Email must match pattern
    Condition emailValid = new JsonPathCondition(
        "$.user.email",
        ".+@.+\\..+",  // Simple email regex
        MatchOperation.REGEX,
        false
    );

    ConditionResult result = matcher.evaluate(emailValid, request);

    if (!result.isMatched()) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Invalid email format"
        ));
    }

    return ResponseEntity.ok(Map.of("status", "registered"));
}
```

---

### Example 4: Contains Matching

**Scenario:** Check if event type starts with "order."

```java
@PostMapping("/events")
public ResponseEntity<?> handleEvent(HttpServletRequest request) {
    // Match any order-related event
    Condition orderEvent = new JsonPathCondition(
        "$.event.type",
        "order.",
        MatchOperation.STARTS_WITH,
        false
    );

    ConditionResult result = matcher.evaluate(orderEvent, request);

    if (result.isMatched()) {
        return handleOrderEvent();
    } else {
        return handleOtherEvent();
    }
}
```

**Matches:**
- `{"event": {"type": "order.created"}}`
- `{"event": {"type": "order.updated"}}`
- `{"event": {"type": "order.cancelled"}}`

**Does not match:**
- `{"event": {"type": "user.created"}}`

---

## JsonExactMatchCondition

Matches multiple specific JSON fields with exact values. More efficient than multiple JsonPathConditions when checking exact values.

**Constructor:**
```java
public JsonExactMatchCondition(
    Map<String, String> expectedFields,  // Field path → expected value
    boolean ignoreCase                   // Case sensitivity
)
```

### Example 1: Multi-Field Validation

**Scenario:** Ensure request has specific tenant and plan

```java
@PostMapping("/premium-feature")
public ResponseEntity<?> premiumFeature(HttpServletRequest request) {
    // Check multiple fields at once
    Map<String, String> requiredFields = Map.of(
        "$.tenant.id", "acme-corp",
        "$.tenant.plan", "premium"
    );

    Condition exactMatch = new JsonExactMatchCondition(
        requiredFields,
        false  // Case sensitive
    );

    ConditionResult result = matcher.evaluate(exactMatch, request);

    if (!result.isMatched()) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Premium feature requires Acme Corp premium plan"
        ));
    }

    return ResponseEntity.ok(Map.of("feature", "enabled"));
}
```

**Testing:**
```bash
# Both fields match (success)
curl -X POST http://localhost:8080/api/premium-feature \
  -H "Content-Type: application/json" \
  -d '{
    "tenant": {"id": "acme-corp", "plan": "premium"},
    "data": {}
  }'

# Wrong tenant (403)
curl -X POST http://localhost:8080/api/premium-feature \
  -H "Content-Type: application/json" \
  -d '{
    "tenant": {"id": "other-corp", "plan": "premium"},
    "data": {}
  }'

# Wrong plan (403)
curl -X POST http://localhost:8080/api/premium-feature \
  -H "Content-Type: application/json" \
  -d '{
    "tenant": {"id": "acme-corp", "plan": "basic"},
    "data": {}
  }'
```

---

### Example 2: Webhook Signature Validation

**Scenario:** Validate webhook payload structure

```java
@PostMapping("/webhook-receiver")
public ResponseEntity<?> receiveWebhook(HttpServletRequest request) {
    // Ensure webhook has required structure
    Map<String, String> webhookStructure = Map.of(
        "$.event.type", "payment.success",
        "$.event.version", "1.0",
        "$.metadata.source", "payment-gateway"
    );

    Condition structureValid = new JsonExactMatchCondition(
        webhookStructure,
        false
    );

    ConditionResult result = matcher.evaluate(structureValid, request);

    if (!result.isMatched()) {
        result.getFailures().forEach(failure ->
            System.err.println("Invalid webhook: " + failure.getMessage())
        );

        return ResponseEntity.badRequest().body(Map.of(
            "error", "Invalid webhook structure"
        ));
    }

    return ResponseEntity.ok(Map.of("status", "received"));
}
```

---

## Combining JSON with Other Conditions

The real power comes from combining JSON matching with header and query parameter conditions.

### Example 1: Tenant + Authentication

**Scenario:** Validate tenant in JSON AND API key in header

```java
@PostMapping("/tenant-api")
public ResponseEntity<?> tenantApi(HttpServletRequest request) {
    // Tenant must be valid
    Condition validTenant = new JsonPathCondition(
        "$.tenant.id",
        "acme-corp",
        MatchOperation.EQUALS,
        false
    );

    // API key must be present
    Condition validApiKey = new HeaderCondition(
        "X-Api-Key",
        "valid-key-123",
        MatchOperation.EQUALS,
        false
    );

    // Both conditions must match
    Condition combined = ConditionGroup.and(validTenant, validApiKey);

    ConditionResult result = matcher.evaluate(combined, request);

    if (!result.isMatched()) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Authentication failed",
            "failures", result.getFailures().stream()
                .map(ConditionFailure::getMessage)
                .collect(Collectors.toList())
        ));
    }

    return ResponseEntity.ok(Map.of("status", "authorized"));
}
```

**Testing:**
```bash
# Both valid (success)
curl -X POST http://localhost:8080/api/tenant-api \
  -H "X-Api-Key: valid-key-123" \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"id": "acme-corp"}}'

# Missing API key (403)
curl -X POST http://localhost:8080/api/tenant-api \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"id": "acme-corp"}}'

# Wrong tenant (403)
curl -X POST http://localhost:8080/api/tenant-api \
  -H "X-Api-Key: valid-key-123" \
  -H "Content-Type: application/json" \
  -d '{"tenant": {"id": "wrong-tenant"}}'
```

---

### Example 2: Priority Routing

**Scenario:** Route high-priority orders differently, but only for premium tenants

```java
@PostMapping("/orders")
public ResponseEntity<?> createOrder(HttpServletRequest request) {
    // Check if premium tenant
    Condition isPremium = new JsonPathCondition(
        "$.tenant.plan",
        "premium",
        MatchOperation.EQUALS,
        false
    );

    // Check if high priority
    Condition isHighPriority = new JsonPathCondition(
        "$.order.priority",
        "high",
        MatchOperation.EQUALS,
        true
    );

    // Premium AND high priority
    Condition priorityProcessing = ConditionGroup.and(isPremium, isHighPriority);

    ConditionResult result = matcher.evaluate(priorityProcessing, request);

    if (result.isMatched()) {
        return processPriorityOrder();
    } else {
        return processStandardOrder();
    }
}

private ResponseEntity<?> processPriorityOrder() {
    return ResponseEntity.ok(Map.of(
        "queue", "priority",
        "estimatedTime", "5 minutes"
    ));
}

private ResponseEntity<?> processStandardOrder() {
    return ResponseEntity.ok(Map.of(
        "queue", "standard",
        "estimatedTime", "24 hours"
    ));
}
```

---

## Complete Multi-Tenant Example

**Scenario:** Route requests to different databases based on tenant configuration

```java
@RestController
@RequestMapping("/api/saas")
public class MultiTenantController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public MultiTenantController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @PostMapping("/data")
    public ResponseEntity<?> handleData(HttpServletRequest request) {
        // Define tenant-specific conditions
        Condition tenantA = new JsonPathCondition(
            "$.tenant.id", "tenant-a", MatchOperation.EQUALS, false
        );
        Condition tenantB = new JsonPathCondition(
            "$.tenant.id", "tenant-b", MatchOperation.EQUALS, false
        );
        Condition tenantC = new JsonPathCondition(
            "$.tenant.id", "tenant-c", MatchOperation.EQUALS, false
        );

        // Route to appropriate handler
        if (matcher.evaluate(tenantA, request).isMatched()) {
            return routeToTenantA();
        } else if (matcher.evaluate(tenantB, request).isMatched()) {
            return routeToTenantB();
        } else if (matcher.evaluate(tenantC, request).isMatched()) {
            return routeToTenantC();
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Unknown tenant"
            ));
        }
    }

    private ResponseEntity<?> routeToTenantA() {
        // Connect to tenant A's database
        return ResponseEntity.ok(Map.of("tenant", "a", "database", "db-a"));
    }

    private ResponseEntity<?> routeToTenantB() {
        // Connect to tenant B's database
        return ResponseEntity.ok(Map.of("tenant", "b", "database", "db-b"));
    }

    private ResponseEntity<?> routeToTenantC() {
        // Connect to tenant C's database
        return ResponseEntity.ok(Map.of("tenant", "c", "database", "db-c"));
    }
}
```

---

## Troubleshooting

### Problem: JSONPath not found

**Symptom:**
```
java.lang.ClassNotFoundException: com.jayway.jsonpath.JsonPath
```

**Solution:** Add JSONPath dependency:
```xml
<dependency>
    <groupId>com.jayway.jsonpath</groupId>
    <artifactId>json-path</artifactId>
    <version>2.9.0</version>
</dependency>
```

---

### Problem: Path not matching despite correct JSON

**Symptom:**
```java
// JSON: {"user": {"email": "test@example.com"}}
// Condition: $.user.email
// Result: Not matched
```

**Solution 1:** Check path syntax
```java
// Correct
"$.user.email"

// Wrong
"user.email"     // Missing $
"$.user['email']"  // Use dot notation for simple fields
```

**Solution 2:** Verify JSON parsing
```java
// Log the parsed JSON
RequestContext context = RequestContext.from(request);
Object json = context.getJsonBody();
System.out.println("Parsed JSON: " + json);
```

**Solution 3:** Check Content-Type header
```bash
# Must include Content-Type
curl -X POST http://localhost:8080/api \
  -H "Content-Type: application/json" \  # Required!
  -d '{"user": {"email": "test@example.com"}}'
```

---

### Problem: Array handling

**Symptom:**
```java
// JSON: {"items": ["a", "b", "c"]}
// Want to match first item
```

**Solution:** Use array index notation
```java
// Match first item
new JsonPathCondition("$.items[0]", "a", MatchOperation.EQUALS, false)

// Match any item (returns array)
new JsonPathCondition("$.items[*]", ".*", MatchOperation.REGEX, false)
```

**Complex example:**
```json
{
  "orders": [
    {"id": 1, "status": "pending"},
    {"id": 2, "status": "completed"}
  ]
}
```

```java
// Match first order status
new JsonPathCondition("$.orders[0].status", "pending", MatchOperation.EQUALS, false)

// Find any completed order (requires advanced JSONPath)
new JsonPathCondition("$.orders[?(@.status == 'completed')]", ..., ...)
```

---

### Problem: Null or missing fields

**Symptom:**
```java
// JSON: {"user": {}}  (email field missing)
// Condition: $.user.email
// Result: Exception or unexpected behavior
```

**Solution:** Check for existence first
```java
// Option 1: Use try-catch
try {
    ConditionResult result = matcher.evaluate(condition, request);
} catch (Exception e) {
    // Handle missing field
    return ResponseEntity.badRequest().body("Missing required field: user.email");
}

// Option 2: Use custom condition (see Custom Conditions tutorial)
public class JsonFieldExistsCondition implements Condition {
    @Override
    public ConditionResult evaluate(RequestContext context) {
        try {
            Object value = JsonPath.read(context.getJsonBody(), jsonPath);
            return ConditionResult.success();
        } catch (PathNotFoundException e) {
            return ConditionResult.failure(...);
        }
    }
}
```

---

## Best Practices

### 1. Validate Content-Type

Always check Content-Type before evaluating JSON conditions:

```java
Condition jsonContentType = new HeaderCondition(
    "Content-Type",
    "application/json",
    MatchOperation.CONTAINS,
    true
);

if (!matcher.evaluate(jsonContentType, request).isMatched()) {
    return ResponseEntity.status(415)
        .body("Content-Type must be application/json");
}
```

### 2. Use Exact Match for Multiple Fields

```java
// Inefficient: Multiple JsonPathConditions
ConditionGroup.and(
    new JsonPathCondition("$.field1", "value1", ...),
    new JsonPathCondition("$.field2", "value2", ...),
    new JsonPathCondition("$.field3", "value3", ...)
)

// Efficient: Single JsonExactMatchCondition
new JsonExactMatchCondition(Map.of(
    "$.field1", "value1",
    "$.field2", "value2",
    "$.field3", "value3"
), false)
```

### 3. Cache Conditions

```java
// Create once, reuse
private static final Condition ACME_TENANT = new JsonPathCondition(
    "$.tenant.id", "acme-corp", MatchOperation.EQUALS, false
);

// Not: Creating per request
Condition condition = new JsonPathCondition(...);  // Bad
```

### 4. Handle Parse Errors Gracefully

```java
try {
    ConditionResult result = matcher.evaluate(jsonCondition, request);
} catch (InvalidJsonException e) {
    return ResponseEntity.badRequest().body(Map.of(
        "error", "Invalid JSON payload"
    ));
}
```

---

## Key Takeaways

1. **JsonPathCondition** extracts JSON values and matches against patterns
2. **JsonExactMatchCondition** efficiently matches multiple exact fields
3. **JSONPath expressions** use dot notation: `$.field.nested.value`
4. **Combine with headers** for multi-factor validation
5. **Validate Content-Type** before evaluating JSON conditions
6. **Cache conditions** for better performance
7. **Handle missing fields** gracefully with try-catch

---

## Next Steps

**Want declarative syntax?**
→ [AOP Annotations](06-aop-annotations.md) - Use `@JUFUMatchConditions` for JSON matching

**Need custom validation logic?**
→ [Custom Conditions](07-custom-conditions.md) - Build your own JSON validators

**See complete examples?**
→ [Complete Examples](08-complete-examples.md) - Multi-tenant SaaS with JSON routing

---

**[← Complex Conditions](04-building-complex-conditions.md)** | **[Back to Index](00-index.md)** | **[Next: AOP Annotations →](06-aop-annotations.md)**
