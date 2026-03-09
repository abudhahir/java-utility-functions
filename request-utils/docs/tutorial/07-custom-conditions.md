# Custom Conditions

**Goal:** Build your own condition classes for business-specific validation logic.

This guide teaches you to extend the condition framework with custom validators for scenarios not covered by built-in conditions.

---

## When to Write Custom Conditions

Built-in conditions handle common cases well:
- Headers: `HeaderCondition`
- Query params: `QueryParamCondition`
- JSON: `JsonPathCondition`, `JsonExactMatchCondition`

**Write custom conditions when:**

1. **Business logic validation:** Rate limiting, working hours, IP whitelisting
2. **External dependencies:** Database lookups, cache checks, service calls
3. **Stateful checks:** Session validation, token verification
4. **Complex algorithms:** Geolocation, cryptographic verification
5. **Performance optimization:** Cached computations, batch checks

---

## The Condition Interface

All conditions implement this simple interface:

```java
package com.cleveloper.jufu.requestutils.condition.core;

@FunctionalInterface
public interface Condition {
    /**
     * Evaluates this condition against the request context.
     *
     * @param context The request context containing headers, params, JSON body
     * @return ConditionResult indicating success or failure with details
     */
    ConditionResult evaluate(RequestContext context);
}
```

**Key points:**
- Single method: `evaluate(RequestContext)`
- Returns: `ConditionResult` (never throws exceptions)
- Stateless: Same condition instance can be reused across threads
- Pure function: No side effects (logging is ok, state mutation is not)

---

## Anatomy of a Custom Condition

Every custom condition follows this pattern:

```java
public class MyCustomCondition implements Condition {

    // 1. Fields: Configuration (immutable)
    private final String configValue;
    private final boolean someFlag;

    // 2. Constructor: Initialize configuration
    public MyCustomCondition(String configValue, boolean someFlag) {
        this.configValue = configValue;
        this.someFlag = someFlag;
    }

    // 3. evaluate(): Core logic
    @Override
    public ConditionResult evaluate(RequestContext context) {
        // Extract data from context
        String headerValue = context.getHeader("Some-Header");

        // Perform validation logic
        boolean matches = performValidation(headerValue);

        // Return result
        if (matches) {
            return ConditionResult.success();
        } else {
            return ConditionResult.failure(
                new ConditionFailure(
                    "MyCustomCondition",           // Condition type
                    "Some-Header",                 // Field name
                    "my-operation",                // Operation
                    configValue,                   // Expected value
                    headerValue,                   // Actual value
                    "Custom validation failed"     // Human-readable message
                )
            );
        }
    }

    private boolean performValidation(String value) {
        // Your logic here
        return value != null && value.contains(configValue);
    }
}
```

---

## Complete Example 1: Working Hours Condition

**Use case:** Only allow requests during business hours (9 AM - 5 PM, Monday-Friday)

```java
package com.example.conditions;

import com.cleveloper.jufu.requestutils.condition.core.Condition;
import com.cleveloper.jufu.requestutils.condition.core.ConditionResult;
import com.cleveloper.jufu.requestutils.condition.core.ConditionFailure;
import com.cleveloper.jufu.requestutils.condition.core.RequestContext;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Condition that only matches during business hours.
 * Configurable start/end times and timezone.
 */
public class WorkingHoursCondition implements Condition {

    private final LocalTime startTime;
    private final LocalTime endTime;
    private final ZoneId timezone;

    /**
     * Creates a working hours condition.
     *
     * @param startHour Start hour (0-23)
     * @param endHour End hour (0-23)
     * @param timezone Timezone for evaluation
     */
    public WorkingHoursCondition(int startHour, int endHour, ZoneId timezone) {
        this.startTime = LocalTime.of(startHour, 0);
        this.endTime = LocalTime.of(endHour, 0);
        this.timezone = timezone;
    }

    /**
     * Convenience constructor with default timezone (UTC).
     */
    public WorkingHoursCondition(int startHour, int endHour) {
        this(startHour, endHour, ZoneId.of("UTC"));
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        LocalDateTime now = LocalDateTime.now(timezone);
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // Check if weekend
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return ConditionResult.failure(
                new ConditionFailure(
                    "WorkingHours",
                    "current-day",
                    "is-weekday",
                    "Monday-Friday",
                    dayOfWeek.toString(),
                    String.format("Request received on weekend (%s). Service available Monday-Friday only.",
                        dayOfWeek)
                )
            );
        }

        // Check if within working hours
        if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
            return ConditionResult.failure(
                new ConditionFailure(
                    "WorkingHours",
                    "current-time",
                    "is-between",
                    String.format("%s-%s", startTime, endTime),
                    currentTime.toString(),
                    String.format("Request received outside working hours. Service available %s-%s %s.",
                        startTime, endTime, timezone.getId())
                )
            );
        }

        // Success
        return ConditionResult.success();
    }

    @Override
    public String toString() {
        return String.format("WorkingHoursCondition[%s-%s %s]",
            startTime, endTime, timezone.getId());
    }
}
```

**Usage:**

```java
@RestController
@RequestMapping("/api")
public class BusinessHoursController {

    private final RequestConditionMatcher matcher;

    // Create condition once
    private static final Condition BUSINESS_HOURS = new WorkingHoursCondition(
        9, 17, ZoneId.of("America/New_York")  // 9 AM - 5 PM EST
    );

    @Autowired
    public BusinessHoursController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/support")
    public ResponseEntity<?> support(HttpServletRequest request) {
        ConditionResult result = matcher.evaluate(BUSINESS_HOURS, request);

        if (!result.isMatched()) {
            ConditionFailure failure = result.getFailures().get(0);
            return ResponseEntity.status(503).body(Map.of(
                "error", "Service unavailable",
                "reason", failure.getMessage(),
                "availableHours", "9:00 AM - 5:00 PM EST, Monday-Friday"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "available",
            "message", "Support team is online"
        ));
    }
}
```

**Testing:**

```bash
# During business hours (success)
curl http://localhost:8080/api/support
# Response: {"status": "available", "message": "Support team is online"}

# Outside business hours (503)
# Response: {
#   "error": "Service unavailable",
#   "reason": "Request received outside working hours. Service available 09:00-17:00 America/New_York.",
#   "availableHours": "9:00 AM - 5:00 PM EST, Monday-Friday"
# }

# Weekend (503)
# Response: {
#   "error": "Service unavailable",
#   "reason": "Request received on weekend (SATURDAY). Service available Monday-Friday only.",
#   "availableHours": "9:00 AM - 5:00 PM EST, Monday-Friday"
# }
```

---

## Example 2: IP Whitelist Condition

**Use case:** Only allow requests from whitelisted IP addresses

```java
package com.example.conditions;

import com.cleveloper.jufu.requestutils.condition.core.*;

import java.util.Set;

/**
 * Condition that only matches requests from whitelisted IP addresses.
 */
public class IpWhitelistCondition implements Condition {

    private final Set<String> allowedIps;

    public IpWhitelistCondition(Set<String> allowedIps) {
        this.allowedIps = Set.copyOf(allowedIps);  // Defensive copy, immutable
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        // Get client IP from X-Forwarded-For header (if behind proxy)
        String forwardedFor = context.getHeader("X-Forwarded-For");
        String clientIp;

        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // X-Forwarded-For can be comma-separated list, take first
            clientIp = forwardedFor.split(",")[0].trim();
        } else {
            // Fallback to X-Real-IP or direct connection
            String realIp = context.getHeader("X-Real-IP");
            clientIp = realIp != null ? realIp : "unknown";
        }

        // Check if IP is whitelisted
        if (allowedIps.contains(clientIp)) {
            return ConditionResult.success();
        }

        return ConditionResult.failure(
            new ConditionFailure(
                "IpWhitelist",
                "client-ip",
                "in-whitelist",
                "One of: " + allowedIps,
                clientIp,
                String.format("IP address %s is not whitelisted. Access denied.", clientIp)
            )
        );
    }

    @Override
    public String toString() {
        return String.format("IpWhitelistCondition[allowed=%s]", allowedIps);
    }
}
```

**Usage:**

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RequestConditionMatcher matcher;

    // Whitelist admin IPs
    private static final Condition ADMIN_IP_WHITELIST = new IpWhitelistCondition(
        Set.of("192.168.1.100", "10.0.0.50", "203.0.113.0")
    );

    @Autowired
    public AdminController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> adminDashboard(HttpServletRequest request) {
        ConditionResult result = matcher.evaluate(ADMIN_IP_WHITELIST, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "Access denied",
                "message", result.getFailures().get(0).getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of("dashboard", "admin-data"));
    }
}
```

---

## Example 3: Rate Limit Condition

**Use case:** Limit requests per user using a cache

```java
package com.example.conditions;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Condition that enforces rate limiting per user.
 * Uses in-memory cache with sliding window.
 */
public class RateLimitCondition implements Condition {

    private final int maxRequests;
    private final Duration window;
    private final Cache<String, AtomicInteger> requestCounts;

    /**
     * @param maxRequests Maximum requests allowed in time window
     * @param window Time window duration
     */
    public RateLimitCondition(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
        this.requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(window)
            .build();
    }

    @Override
    public ConditionResult evaluate(RequestContext context) {
        // Extract user identifier (API key, user ID, IP, etc.)
        String userId = getUserIdentifier(context);

        if (userId == null) {
            return ConditionResult.failure(
                new ConditionFailure(
                    "RateLimit",
                    "user-id",
                    "exists",
                    "non-null",
                    "null",
                    "Unable to identify user for rate limiting"
                )
            );
        }

        // Get or create counter for this user
        AtomicInteger count = requestCounts.get(userId, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        // Check if exceeded limit
        if (currentCount > maxRequests) {
            return ConditionResult.failure(
                new ConditionFailure(
                    "RateLimit",
                    "request-count",
                    "less-than-or-equal",
                    String.valueOf(maxRequests),
                    String.valueOf(currentCount),
                    String.format("Rate limit exceeded. Maximum %d requests per %s.",
                        maxRequests, formatDuration(window))
                )
            );
        }

        return ConditionResult.success();
    }

    private String getUserIdentifier(RequestContext context) {
        // Try multiple identification methods
        String apiKey = context.getHeader("X-Api-Key");
        if (apiKey != null) return apiKey;

        String userId = context.getHeader("X-User-Id");
        if (userId != null) return userId;

        String ip = context.getHeader("X-Forwarded-For");
        if (ip != null) return ip.split(",")[0].trim();

        return null;
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return (seconds / 60) + " minutes";
        return (seconds / 3600) + " hours";
    }

    @Override
    public String toString() {
        return String.format("RateLimitCondition[max=%d, window=%s]",
            maxRequests, formatDuration(window));
    }
}
```

**Dependency (add to pom.xml):**

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
```

**Usage:**

```java
@RestController
@RequestMapping("/api")
public class RateLimitedController {

    private final RequestConditionMatcher matcher;

    // 10 requests per minute
    private static final Condition RATE_LIMIT = new RateLimitCondition(
        10, Duration.ofMinutes(1)
    );

    @Autowired
    public RateLimitedController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    @GetMapping("/data")
    public ResponseEntity<?> getData(HttpServletRequest request) {
        ConditionResult result = matcher.evaluate(RATE_LIMIT, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Too many requests",
                "message", result.getFailures().get(0).getMessage(),
                "retryAfter", "60 seconds"
            ));
        }

        return ResponseEntity.ok(Map.of("data", "response"));
    }
}
```

---

## Testing Custom Conditions

### Unit Testing

Custom conditions are easy to test - they're pure functions:

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkingHoursConditionTest {

    @Test
    void shouldMatchDuringBusinessHours() {
        // This test is time-dependent - use a clock abstraction in production
        Condition condition = new WorkingHoursCondition(9, 17, ZoneId.of("UTC"));

        // Create mock context (conditions don't need real HTTP request)
        RequestContext context = new MockRequestContext();

        ConditionResult result = condition.evaluate(context);

        // Assert based on current time
        // In production, inject Clock and use Clock.fixed() for testing
    }

    @Test
    void shouldFailOnWeekend() {
        // Create a testable version that accepts Clock
        // Clock fixedClock = Clock.fixed(
        //     Instant.parse("2024-01-06T10:00:00Z"),  // Saturday
        //     ZoneId.of("UTC")
        // );
        //
        // Condition condition = new WorkingHoursCondition(9, 17, fixedClock);
        // ConditionResult result = condition.evaluate(context);
        //
        // assertFalse(result.isMatched());
        // assertTrue(result.getFailures().get(0).getMessage().contains("weekend"));
    }
}
```

**Mock RequestContext:**

```java
class MockRequestContext implements RequestContext {
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private Object jsonBody;

    public MockRequestContext withHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public MockRequestContext withQueryParam(String name, String value) {
        queryParams.put(name, value);
        return this;
    }

    public MockRequestContext withJsonBody(Object body) {
        this.jsonBody = body;
        return this;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    @Override
    public Object getJsonBody() {
        return jsonBody;
    }
}
```

---

### Integration Testing

Test custom conditions in real controllers:

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowWhitelistedIp() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("X-Forwarded-For", "192.168.1.100"))  // Whitelisted IP
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dashboard").value("admin-data"));
    }

    @Test
    void shouldBlockNonWhitelistedIp() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("X-Forwarded-For", "1.2.3.4"))  // Not whitelisted
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("Access denied"));
    }
}
```

---

## Performance Considerations

### 1. Avoid External Calls in evaluate()

**Bad:**
```java
@Override
public ConditionResult evaluate(RequestContext context) {
    // Synchronous database call on every request
    boolean isValid = database.checkUser(context.getHeader("X-User-Id"));
    return isValid ? ConditionResult.success() : ConditionResult.failure(...);
}
```

**Good:**
```java
// Cache database lookups
private final Cache<String, Boolean> userCache = Caffeine.newBuilder()
    .expireAfterWrite(Duration.ofMinutes(5))
    .build();

@Override
public ConditionResult evaluate(RequestContext context) {
    String userId = context.getHeader("X-User-Id");
    Boolean isValid = userCache.get(userId, id -> database.checkUser(id));
    return isValid ? ConditionResult.success() : ConditionResult.failure(...);
}
```

### 2. Make Conditions Stateless

**Bad:**
```java
public class StatefulCondition implements Condition {
    private int requestCount = 0;  // State - breaks thread safety!

    @Override
    public ConditionResult evaluate(RequestContext context) {
        requestCount++;  // Race condition
        return requestCount < 100 ? success() : failure();
    }
}
```

**Good:**
```java
public class StatelessCondition implements Condition {
    private final Cache<String, AtomicInteger> counts;  // External state management

    @Override
    public ConditionResult evaluate(RequestContext context) {
        // No instance state modification
        String key = context.getHeader("X-User-Id");
        AtomicInteger count = counts.get(key, k -> new AtomicInteger(0));
        return count.incrementAndGet() < 100 ? success() : failure();
    }
}
```

### 3. Optimize for FAIL_FAST

If your condition is likely to fail, fail early:

```java
@Override
public ConditionResult evaluate(RequestContext context) {
    // Check cheapest condition first
    String header = context.getHeader("X-Api-Key");
    if (header == null) {
        return ConditionResult.failure(...);  // Fast failure
    }

    // Only do expensive check if header exists
    boolean isValid = expensiveValidation(header);
    return isValid ? ConditionResult.success() : ConditionResult.failure(...);
}
```

### 4. Reuse Condition Instances

```java
// Good: Create once, reuse everywhere
private static final Condition WORKING_HOURS = new WorkingHoursCondition(9, 17);

// Bad: Creating new instances per request
Condition condition = new WorkingHoursCondition(9, 17);  // Wasteful
```

---

## Best Practices

### 1. Implement toString()

Helpful for debugging and logging:

```java
@Override
public String toString() {
    return String.format("MyCondition[param1=%s, param2=%s]", param1, param2);
}
```

### 2. Use Descriptive Failure Messages

```java
// Good: Actionable message
"IP address 1.2.3.4 is not whitelisted. Contact admin@example.com to request access."

// Bad: Vague message
"Access denied"
```

### 3. Make Fields Final and Immutable

```java
public class MyCondition implements Condition {
    private final String config;  // final = immutable
    private final List<String> values;

    public MyCondition(String config, List<String> values) {
        this.config = config;
        this.values = List.copyOf(values);  // Defensive copy
    }
}
```

### 4. Document Expected Headers/Context

```java
/**
 * Validates API keys against a whitelist.
 *
 * <p>Expected context:
 * <ul>
 *   <li>Header: X-Api-Key (required)</li>
 *   <li>Header: X-User-Id (optional, for logging)</li>
 * </ul>
 *
 * @see ApiKeyValidator
 */
public class ApiKeyCondition implements Condition {
    // ...
}
```

---

## Key Takeaways

1. **Implement Condition interface** with single `evaluate()` method
2. **Return ConditionResult** - never throw exceptions from evaluate()
3. **Make conditions stateless and immutable** for thread safety
4. **Cache external lookups** to avoid performance issues
5. **Provide detailed ConditionFailure** messages for debugging
6. **Unit test with MockRequestContext** for fast feedback
7. **Reuse instances** - create once, evaluate many times
8. **Document expected context** (headers, params) in JavaDoc

---

## Next Steps

**See custom conditions in action:**
→ [Complete Examples](08-complete-examples.md) - Real-world custom condition usage

**Need to debug?**
→ [Troubleshooting](09-troubleshooting.md) - Common custom condition issues

**Want to see all features together?**
→ [Complete Examples](08-complete-examples.md) - Combine custom with built-in conditions

---

**[← AOP Annotations](06-aop-annotations.md)** | **[Back to Index](00-index.md)** | **[Next: Complete Examples →](08-complete-examples.md)**
