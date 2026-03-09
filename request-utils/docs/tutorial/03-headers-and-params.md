# Headers and Query Parameters

**Goal:** Master practical matching scenarios for HTTP headers and query parameters.

This guide covers real-world patterns you'll use daily. After reading, you'll confidently handle API keys, content negotiation, feature flags, versioning, and more.

---

## Headers vs Query Parameters

Both use identical APIs - the only difference is the data source:

```java
// Header matching
Condition headerCondition = new HeaderCondition(
    "X-Api-Version",              // Header name
    "v2",                         // Expected value
    MatchOperation.EQUALS,        // Match operation
    false                         // Case sensitive
);

// Query parameter matching
Condition queryCondition = new QueryParamCondition(
    "api_version",                // Parameter name
    "v2",                         // Expected value
    MatchOperation.EQUALS,        // Match operation
    false                         // Case sensitive
);
```

**When to use each:**
- **Headers:** Authentication, content negotiation, client metadata, versioning
- **Query Parameters:** Filters, pagination, feature flags, public-facing toggles

---

## Common Scenarios

### 1. API Key Validation

**Use case:** Validate API keys in production requests

```java
@RestController
@RequestMapping("/api/v1")
public class SecureApiController {

    private final RequestConditionMatcher matcher;
    private static final String VALID_API_KEY = "sk-prod-abc123xyz";

    @Autowired
    public SecureApiController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @PostMapping("/data")
    public ResponseEntity<?> handleSecureRequest(HttpServletRequest request) {
        // API key must be present and exact match
        Condition apiKeyCondition = new HeaderCondition(
            "X-Api-Key",
            VALID_API_KEY,
            MatchOperation.EQUALS,
            false  // Case sensitive for security
        );

        ConditionResult result = matcher.evaluate(apiKeyCondition, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(401)
                .body(Map.of(
                    "error", "Unauthorized",
                    "message", "Invalid or missing API key"
                ));
        }

        // Process secure request
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
```

**Testing:**
```bash
# Valid key
curl -H "X-Api-Key: sk-prod-abc123xyz" \
  http://localhost:8080/api/v1/data

# Invalid key (401 response)
curl -H "X-Api-Key: wrong-key" \
  http://localhost:8080/api/v1/data
```

**Production tip:** Don't hardcode API keys. Use environment variables or a secret management service:

```java
@Value("${api.key}")
private String validApiKey;

Condition apiKeyCondition = new HeaderCondition(
    "X-Api-Key",
    validApiKey,  // From configuration
    MatchOperation.EQUALS,
    false
);
```

---

### 2. Content Negotiation

**Use case:** Route requests based on Accept header

```java
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public ContentController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/data")
    public ResponseEntity<?> getData(HttpServletRequest request) {
        // Check if client wants JSON
        Condition jsonCondition = new HeaderCondition(
            "Accept",
            "application/json",
            MatchOperation.CONTAINS,  // May include charset, etc.
            true  // Case insensitive
        );

        // Check if client wants XML
        Condition xmlCondition = new HeaderCondition(
            "Accept",
            "application/xml",
            MatchOperation.CONTAINS,
            true
        );

        ConditionResult jsonResult = matcher.evaluate(jsonCondition, request);
        ConditionResult xmlResult = matcher.evaluate(xmlCondition, request);

        if (jsonResult.isMatched()) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("format", "json", "data", getData()));
        } else if (xmlResult.isMatched()) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body("<response><format>xml</format></response>");
        } else {
            return ResponseEntity.status(406)
                .body("Unsupported content type. Use application/json or application/xml");
        }
    }

    private Object getData() {
        return Map.of("id", 1, "name", "Example");
    }
}
```

**Why CONTAINS?** Accept headers often include additional information:
```
Accept: application/json, text/plain, */*
Accept: application/json; charset=utf-8
```

**Testing:**
```bash
# Request JSON
curl -H "Accept: application/json" \
  http://localhost:8080/api/content/data

# Request XML
curl -H "Accept: application/xml" \
  http://localhost:8080/api/content/data

# Unsupported (406 response)
curl -H "Accept: text/html" \
  http://localhost:8080/api/content/data
```

---

### 3. Feature Flags

**Use case:** Enable features for specific users or environments

```java
@RestController
@RequestMapping("/api/features")
public class FeatureFlagController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public FeatureFlagController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/beta-feature")
    public ResponseEntity<?> betaFeature(HttpServletRequest request) {
        // Check for beta flag in query parameter
        Condition betaFlagCondition = new QueryParamCondition(
            "beta",
            "true",
            MatchOperation.EQUALS,
            true  // Case insensitive ("True", "TRUE" also work)
        );

        ConditionResult result = matcher.evaluate(betaFlagCondition, request);

        if (result.isMatched()) {
            return ResponseEntity.ok(Map.of(
                "feature", "beta",
                "status", "enabled",
                "message", "You're using the new beta feature!"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "feature", "stable",
                "status", "default",
                "message", "Using stable version"
            ));
        }
    }

    @GetMapping("/admin-panel")
    public ResponseEntity<?> adminPanel(HttpServletRequest request) {
        // Admin flag in header (more secure than query param)
        Condition adminCondition = new HeaderCondition(
            "X-Admin-Access",
            "granted",
            MatchOperation.EQUALS,
            false
        );

        ConditionResult result = matcher.evaluate(adminCondition, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Access denied"));
        }

        return ResponseEntity.ok(Map.of("panel", "admin"));
    }
}
```

**Testing:**
```bash
# Enable beta feature via query parameter
curl "http://localhost:8080/api/features/beta-feature?beta=true"

# Default behavior without flag
curl "http://localhost:8080/api/features/beta-feature"

# Admin access via header
curl -H "X-Admin-Access: granted" \
  http://localhost:8080/api/features/admin-panel
```

**Header vs Query Parameter for flags:**
- **Query parameters:** User-visible, shareable URLs, easy to toggle
- **Headers:** Hidden from casual users, better for security-sensitive flags

---

### 4. API Versioning

**Use case:** Route requests to different handlers based on version

```java
@RestController
@RequestMapping("/api")
public class VersionedApiController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public VersionedApiController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(HttpServletRequest request) {
        // Check for v2 via header
        Condition v2HeaderCondition = new HeaderCondition(
            "X-Api-Version",
            "v2",
            MatchOperation.EQUALS,
            false
        );

        // Check for v2 via query parameter
        Condition v2QueryCondition = new QueryParamCondition(
            "version",
            "v2",
            MatchOperation.EQUALS,
            true
        );

        // Accept either header or query param for v2
        ConditionResult headerResult = matcher.evaluate(v2HeaderCondition, request);
        ConditionResult queryResult = matcher.evaluate(v2QueryCondition, request);

        if (headerResult.isMatched() || queryResult.isMatched()) {
            return handleV2Request();
        } else {
            return handleV1Request();
        }
    }

    private ResponseEntity<?> handleV2Request() {
        // V2 response format: includes pagination metadata
        return ResponseEntity.ok(Map.of(
            "version", "v2",
            "data", List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
                Map.of("id", 2, "name", "Bob", "email", "bob@example.com")
            ),
            "pagination", Map.of("page", 1, "total", 2)
        ));
    }

    private ResponseEntity<?> handleV1Request() {
        // V1 response format: simple array
        return ResponseEntity.ok(List.of(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob")
        ));
    }
}
```

**Testing:**
```bash
# V2 via header
curl -H "X-Api-Version: v2" \
  http://localhost:8080/api/users

# V2 via query parameter
curl "http://localhost:8080/api/users?version=v2"

# V1 (default)
curl http://localhost:8080/api/users
```

**Version header patterns:**
```java
// Exact version match
new HeaderCondition("X-Api-Version", "v2", MatchOperation.EQUALS, false)

// Semantic versioning (v2.1, v2.2, etc.)
new HeaderCondition("X-Api-Version", "v2\\.", MatchOperation.REGEX, false)

// Major version only
new HeaderCondition("X-Api-Version", "v2", MatchOperation.STARTS_WITH, false)
```

---

### 5. User-Agent Detection

**Use case:** Detect mobile clients and serve optimized content

```java
@RestController
@RequestMapping("/api/content")
public class UserAgentController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public UserAgentController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/page")
    public ResponseEntity<?> servePage(HttpServletRequest request) {
        // Check for mobile user agents
        Condition mobileCondition = new HeaderCondition(
            "User-Agent",
            ".*(Mobile|Android|iPhone|iPad).*",
            MatchOperation.REGEX,
            true  // Case insensitive
        );

        ConditionResult result = matcher.evaluate(mobileCondition, request);

        if (result.isMatched()) {
            return ResponseEntity.ok(Map.of(
                "layout", "mobile",
                "content", "Mobile-optimized content",
                "images", "low-resolution"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "layout", "desktop",
                "content", "Full desktop content",
                "images", "high-resolution"
            ));
        }
    }
}
```

**Common User-Agent patterns:**
```java
// Mobile devices
".*Mobile.*"

// Specific platforms
".*Android.*"
".*iPhone.*"
".*iPad.*"

// Desktop browsers
".*Chrome.*"
".*Firefox.*"
".*Safari.*"

// Bot detection
".*bot.*|.*crawler.*|.*spider.*"
```

**Testing:**
```bash
# Mobile request
curl -H "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)" \
  http://localhost:8080/api/content/page

# Desktop request
curl -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)" \
  http://localhost:8080/api/content/page
```

---

### 6. Environment-Based Routing

**Use case:** Enable debug features in development only

```java
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private final RequestConditionMatcher matcher;

    @Autowired
    public DebugController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/info")
    public ResponseEntity<?> debugInfo(HttpServletRequest request) {
        // Check for debug header (set by reverse proxy in dev environment)
        Condition debugCondition = new HeaderCondition(
            "X-Environment",
            "development",
            MatchOperation.EQUALS,
            true
        );

        ConditionResult result = matcher.evaluate(debugCondition, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Debug features disabled in production"));
        }

        // Return sensitive debug information
        return ResponseEntity.ok(Map.of(
            "jvmMemory", Runtime.getRuntime().totalMemory(),
            "activeThreads", Thread.activeCount(),
            "systemProperties", System.getProperties()
        ));
    }
}
```

---

## Side-by-Side Comparison

Here's the same logic implemented with headers and query parameters:

### Header-Based Implementation
```java
@GetMapping("/premium-content")
public ResponseEntity<?> premiumContentViaHeader(HttpServletRequest request) {
    Condition condition = new HeaderCondition(
        "X-Subscription-Tier",
        "premium",
        MatchOperation.EQUALS,
        false
    );

    ConditionResult result = matcher.evaluate(condition, request);

    if (result.isMatched()) {
        return ResponseEntity.ok("Premium content");
    }
    return ResponseEntity.status(403).body("Premium subscription required");
}
```

**Call:** `curl -H "X-Subscription-Tier: premium" http://localhost:8080/api/premium-content`

### Query Parameter Implementation
```java
@GetMapping("/premium-content")
public ResponseEntity<?> premiumContentViaQuery(HttpServletRequest request) {
    Condition condition = new QueryParamCondition(
        "tier",
        "premium",
        MatchOperation.EQUALS,
        false
    );

    ConditionResult result = matcher.evaluate(condition, request);

    if (result.isMatched()) {
        return ResponseEntity.ok("Premium content");
    }
    return ResponseEntity.status(403).body("Premium subscription required");
}
```

**Call:** `curl "http://localhost:8080/api/premium-content?tier=premium"`

**The only difference:** `HeaderCondition` vs `QueryParamCondition` and data source.

---

## Troubleshooting

### Problem: Header not matching despite being present

**Symptom:**
```java
Condition condition = new HeaderCondition("content-type", "application/json", ...);
// Always fails even when Content-Type header is present
```

**Solution:** HTTP header names are case-insensitive but may be normalized differently. Always use standard casing:

```java
// Correct
new HeaderCondition("Content-Type", "application/json", ...)

// May fail
new HeaderCondition("content-type", "application/json", ...)
```

**Debug approach:**
```java
// Log all headers to see exact names
request.getHeaderNames().asIterator().forEachRemaining(name ->
    System.out.println(name + ": " + request.getHeader(name))
);
```

---

### Problem: Query parameter with special characters

**Symptom:**
```bash
curl "http://localhost:8080/api?filter=status:active"
# Parameter value is "status:active" but condition doesn't match
```

**Solution:** URL-encode special characters:

```bash
# Correct
curl "http://localhost:8080/api?filter=status%3Aactive"
```

Or match more flexibly:
```java
// Use CONTAINS for query params with special chars
new QueryParamCondition("filter", "active", MatchOperation.CONTAINS, true)
```

---

### Problem: Case sensitivity confusion

**Symptom:**
```java
new HeaderCondition("X-Api-Key", "ABC123", MatchOperation.EQUALS, true)
// Matches "abc123" when you expected case-sensitive match
```

**Solution:** Fourth parameter controls case sensitivity:
- `false` = case sensitive (default for security)
- `true` = case insensitive

```java
// Case sensitive (recommended for API keys, tokens)
new HeaderCondition("X-Api-Key", "ABC123", MatchOperation.EQUALS, false)

// Case insensitive (safe for tier names, feature flags)
new QueryParamCondition("tier", "premium", MatchOperation.EQUALS, true)
```

---

### Problem: Empty or null values

**Symptom:**
```java
// Header is present but empty
curl -H "X-User-Type: " http://localhost:8080/api/feature
```

**Solution:** Check for presence separately:

```java
String headerValue = request.getHeader("X-User-Type");
if (headerValue == null || headerValue.isEmpty()) {
    return ResponseEntity.badRequest()
        .body("X-User-Type header is required");
}

// Now evaluate condition
Condition condition = new HeaderCondition("X-User-Type", "premium", ...);
```

Or use custom condition (see [Custom Conditions](07-custom-conditions.md)) to check presence.

---

## Best Practices

### 1. Security-Sensitive Values

Use case-sensitive matching for security tokens:
```java
// API keys, tokens, signatures
new HeaderCondition("X-Api-Key", key, MatchOperation.EQUALS, false)
```

### 2. User-Facing Values

Use case-insensitive for user convenience:
```java
// Feature flags, tier names
new QueryParamCondition("tier", "premium", MatchOperation.EQUALS, true)
```

### 3. Reuse Conditions

Create once, reuse everywhere:
```java
@Component
public class CommonConditions {
    public static final Condition IS_PREMIUM = new HeaderCondition(
        "X-Subscription-Tier", "premium", MatchOperation.EQUALS, false
    );

    public static final Condition IS_MOBILE = new HeaderCondition(
        "User-Agent", ".*Mobile.*", MatchOperation.REGEX, true
    );
}

// Use in controllers
@Autowired
private CommonConditions conditions;

ConditionResult result = matcher.evaluate(CommonConditions.IS_PREMIUM, request);
```

### 4. Choose the Right Operation

| Scenario | Operation | Example |
|----------|-----------|---------|
| Exact match | `EQUALS` | API key: "sk-prod-123" |
| Prefix check | `STARTS_WITH` | Version: "v2" in "v2.1.3" |
| Suffix check | `ENDS_WITH` | Content-Type: "json" in "application/json" |
| Substring | `CONTAINS` | User-Agent: "Mobile" |
| Complex pattern | `REGEX` | Email: ".+@.+\\.com" |

### 5. Document Your Conditions

```java
/**
 * Validates premium subscription access.
 * Checks X-Subscription-Tier header for exact match "premium".
 *
 * @param request HTTP request with subscription header
 * @return 200 if premium, 403 otherwise
 */
@GetMapping("/premium-feature")
public ResponseEntity<?> premiumFeature(HttpServletRequest request) {
    // ...
}
```

---

## Next Steps

**Ready to combine multiple conditions?**
→ [Building Complex Conditions](04-building-complex-conditions.md) - AND/OR logic and nesting

**Need to match JSON payloads?**
→ [JSON Matching](05-json-matching.md) - JSONPath support

**Want to see complete examples?**
→ [Complete Examples](08-complete-examples.md) - Real-world applications

---

**[← Core Concepts](02-core-concepts.md)** | **[Back to Index](00-index.md)** | **[Next: Complex Conditions →](04-building-complex-conditions.md)**
