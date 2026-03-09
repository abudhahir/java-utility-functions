# Complete Examples

**Goal:** See request-utils in action with four complete, production-ready applications.

This guide provides end-to-end examples you can copy, run, and adapt. Each includes full code, tests, and design decisions.

---

## Example 1: API Versioning System

**Scenario:** Route requests to different API versions based on header or query parameter.

### Requirements

- Support v1 and v2 endpoints
- Accept version in `X-Api-Version` header OR `version` query param
- V1: Legacy format (simple response)
- V2: New format (includes metadata, pagination)
- Default to v1 for backwards compatibility

### File Structure

```
src/main/java/com/example/versioning/
├── VersionedApiController.java
├── dto/
│   ├── V1Response.java
│   └── V2Response.java
└── service/
    └── UserService.java

src/test/java/com/example/versioning/
└── VersionedApiControllerTest.java
```

### Implementation

**V1Response.java**
```java
package com.example.versioning.dto;

import java.util.List;

public record V1Response(List<User> users) {
    public record User(Long id, String name) {}
}
```

**V2Response.java**
```java
package com.example.versioning.dto;

import java.util.List;

public record V2Response(
    String version,
    List<User> data,
    Metadata metadata
) {
    public record User(Long id, String name, String email, String status) {}
    public record Metadata(int page, int pageSize, long total) {}
}
```

**UserService.java**
```java
package com.example.versioning.service;

import com.example.versioning.dto.V1Response;
import com.example.versioning.dto.V2Response;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    public V1Response getUsersV1() {
        return new V1Response(List.of(
            new V1Response.User(1L, "Alice"),
            new V1Response.User(2L, "Bob")
        ));
    }

    public V2Response getUsersV2(int page, int pageSize) {
        return new V2Response(
            "v2",
            List.of(
                new V2Response.User(1L, "Alice", "alice@example.com", "active"),
                new V2Response.User(2L, "Bob", "bob@example.com", "active")
            ),
            new V2Response.Metadata(page, pageSize, 2)
        );
    }
}
```

**VersionedApiController.java**
```java
package com.example.versioning;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import com.example.versioning.dto.V1Response;
import com.example.versioning.dto.V2Response;
import com.example.versioning.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API controller supporting multiple versions.
 * Accepts version via header (X-Api-Version) or query param (version).
 */
@RestController
@RequestMapping("/api/users")
public class VersionedApiController {

    private final RequestConditionMatcher matcher;
    private final UserService userService;

    // Pre-defined version conditions
    private static final Condition V2_HEADER = new HeaderCondition(
        "X-Api-Version", "v2", MatchOperation.EQUALS, false
    );

    private static final Condition V2_QUERY = new QueryParamCondition(
        "version", "v2", MatchOperation.EQUALS, true
    );

    // Accept v2 via header OR query param
    private static final Condition IS_V2 = ConditionGroup.or(V2_HEADER, V2_QUERY);

    @Autowired
    public VersionedApiController(
            RequestConditionMatcher matcher,
            UserService userService
    ) {
        this.matcher = matcher;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> getUsers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        ConditionResult result = matcher.evaluate(IS_V2, request);

        if (result.isMatched()) {
            // V2 endpoint
            V2Response response = userService.getUsersV2(page, pageSize);
            return ResponseEntity.ok()
                .header("X-Api-Version", "v2")
                .body(response);
        } else {
            // V1 endpoint (default)
            V1Response response = userService.getUsersV1();
            return ResponseEntity.ok()
                .header("X-Api-Version", "v1")
                .body(response);
        }
    }

    /**
     * V2-only endpoint (requires explicit v2 request).
     */
    @GetMapping("/detailed")
    public ResponseEntity<?> getDetailedUsers(HttpServletRequest request) {
        ConditionResult result = matcher.evaluate(IS_V2, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(400).body(
                "This endpoint requires API version v2. " +
                "Set X-Api-Version: v2 header or version=v2 query parameter."
            );
        }

        V2Response response = userService.getUsersV2(1, 10);
        return ResponseEntity.ok()
            .header("X-Api-Version", "v2")
            .body(response);
    }
}
```

### Tests

**VersionedApiControllerTest.java**
```java
package com.example.versioning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VersionedApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnV1ByDefault() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Api-Version", "v1"))
            .andExpect(jsonPath("$.users").isArray())
            .andExpect(jsonPath("$.users[0].id").value(1))
            .andExpect(jsonPath("$.users[0].name").value("Alice"))
            .andExpect(jsonPath("$.users[0].email").doesNotExist());  // V1 doesn't include email
    }

    @Test
    void shouldReturnV2WithHeader() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("X-Api-Version", "v2"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Api-Version", "v2"))
            .andExpect(jsonPath("$.version").value("v2"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].email").value("alice@example.com"))  // V2 includes email
            .andExpect(jsonPath("$.metadata.page").value(1));
    }

    @Test
    void shouldReturnV2WithQueryParam() throws Exception {
        mockMvc.perform(get("/api/users?version=v2"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Api-Version", "v2"))
            .andExpect(jsonPath("$.version").value("v2"));
    }

    @Test
    void shouldRequireV2ForDetailedEndpoint() throws Exception {
        // V1 request (rejected)
        mockMvc.perform(get("/api/users/detailed"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("requires API version v2")));

        // V2 request (accepted)
        mockMvc.perform(get("/api/users/detailed")
                .header("X-Api-Version", "v2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("v2"));
    }
}
```

### Design Decisions

1. **OR logic for version detection:** Accept header OR query param for flexibility
2. **Default to v1:** Backwards compatibility for existing clients
3. **Explicit v2 endpoints:** `/detailed` requires v2, ensuring new features don't break old clients
4. **Response headers:** Include `X-Api-Version` in response for debugging
5. **DTO separation:** V1 and V2 use different DTOs to prevent accidental breaking changes

---

## Example 2: Multi-Tenant SaaS Router

**Scenario:** Route requests to tenant-specific databases based on JSON payload.

### Requirements

- Extract tenant ID from `$.tenant.id` in request body
- Route to appropriate database connection
- Support premium vs basic tier routing
- Validate tenant exists before processing

### File Structure

```
src/main/java/com/example/saas/
├── TenantController.java
├── TenantService.java
├── TenantRepository.java
├── config/
│   ├── TenantDataSourceConfig.java
│   └── TenantContext.java
└── dto/
    └── TenantRequest.java
```

### Implementation

**TenantRequest.java**
```java
package com.example.saas.dto;

public record TenantRequest(
    Tenant tenant,
    String action,
    Object data
) {
    public record Tenant(String id, String plan) {}
}
```

**TenantContext.java**
```java
package com.example.saas.config;

/**
 * Thread-local storage for current tenant ID.
 */
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
```

**TenantService.java**
```java
package com.example.saas;

import com.example.saas.config.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TenantService {

    // Simulate tenant database routing
    private static final Map<String, String> TENANT_DATABASES = Map.of(
        "tenant-a", "jdbc:postgresql://db-a.example.com/tenantA",
        "tenant-b", "jdbc:postgresql://db-b.example.com/tenantB",
        "tenant-c", "jdbc:postgresql://db-c.example.com/tenantC"
    );

    public Map<String, Object> processTenantRequest(String action, Object data) {
        String tenantId = TenantContext.getTenantId();
        String database = TENANT_DATABASES.get(tenantId);

        if (database == null) {
            throw new IllegalStateException("Unknown tenant: " + tenantId);
        }

        // Simulate database operation
        return Map.of(
            "tenantId", tenantId,
            "database", database,
            "action", action,
            "result", "success"
        );
    }

    public boolean isPremiumTenant(String tenantId) {
        // In production, check database or cache
        return "tenant-a".equals(tenantId) || "tenant-c".equals(tenantId);
    }
}
```

**TenantController.java**
```java
package com.example.saas;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import com.example.saas.config.TenantContext;
import com.example.saas.dto.TenantRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Multi-tenant API controller with tenant-based routing.
 */
@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final RequestConditionMatcher matcher;
    private final TenantService tenantService;

    // Define known tenants
    private static final Condition TENANT_A = new JsonPathCondition(
        "$.tenant.id", "tenant-a", MatchOperation.EQUALS, false
    );

    private static final Condition TENANT_B = new JsonPathCondition(
        "$.tenant.id", "tenant-b", MatchOperation.EQUALS, false
    );

    private static final Condition TENANT_C = new JsonPathCondition(
        "$.tenant.id", "tenant-c", MatchOperation.EQUALS, false
    );

    private static final Condition VALID_TENANT = ConditionGroup.or(
        TENANT_A, TENANT_B, TENANT_C
    );

    @Autowired
    public TenantController(
            RequestConditionMatcher matcher,
            TenantService tenantService
    ) {
        this.matcher = matcher;
        this.tenantService = tenantService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(
            @RequestBody TenantRequest request,
            HttpServletRequest httpRequest
    ) {
        // Validate tenant exists
        ConditionResult result = matcher.evaluate(VALID_TENANT, httpRequest);

        if (!result.isMatched()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid or unknown tenant",
                "tenantId", request.tenant().id()
            ));
        }

        try {
            // Set tenant context for database routing
            TenantContext.setTenantId(request.tenant().id());

            // Process request with tenant-specific database
            var response = tenantService.processTenantRequest(
                request.action(),
                request.data()
            );

            return ResponseEntity.ok(response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Premium-only feature endpoint.
     */
    @PostMapping("/premium-feature")
    public ResponseEntity<?> premiumFeature(
            @RequestBody TenantRequest request,
            HttpServletRequest httpRequest
    ) {
        // Check if premium tenant
        Condition isPremium = new JsonPathCondition(
            "$.tenant.plan", "premium", MatchOperation.EQUALS, true
        );

        ConditionResult result = matcher.evaluate(
            ConditionGroup.and(VALID_TENANT, isPremium),
            httpRequest
        );

        if (!result.isMatched()) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "This feature requires a premium plan"
            ));
        }

        try {
            TenantContext.setTenantId(request.tenant().id());
            return ResponseEntity.ok(Map.of(
                "feature", "premium",
                "tenantId", request.tenant().id()
            ));
        } finally {
            TenantContext.clear();
        }
    }
}
```

### Tests

```java
package com.example.saas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldProcessValidTenant() throws Exception {
        String json = """
            {
                "tenant": {"id": "tenant-a", "plan": "premium"},
                "action": "getData",
                "data": {}
            }
            """;

        mockMvc.perform(post("/api/tenant/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value("tenant-a"))
            .andExpect(jsonPath("$.result").value("success"));
    }

    @Test
    void shouldRejectInvalidTenant() throws Exception {
        String json = """
            {
                "tenant": {"id": "unknown-tenant", "plan": "basic"},
                "action": "getData",
                "data": {}
            }
            """;

        mockMvc.perform(post("/api/tenant/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid or unknown tenant"));
    }

    @Test
    void shouldAllowPremiumFeatureForPremiumTenant() throws Exception {
        String json = """
            {
                "tenant": {"id": "tenant-a", "plan": "premium"},
                "action": "premiumAction",
                "data": {}
            }
            """;

        mockMvc.perform(post("/api/tenant/premium-feature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.feature").value("premium"));
    }

    @Test
    void shouldBlockPremiumFeatureForBasicTenant() throws Exception {
        String json = """
            {
                "tenant": {"id": "tenant-b", "plan": "basic"},
                "action": "premiumAction",
                "data": {}
            }
            """;

        mockMvc.perform(post("/api/tenant/premium-feature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("This feature requires a premium plan"));
    }
}
```

### Design Decisions

1. **ThreadLocal context:** Store tenant ID per request thread for database routing
2. **Validation first:** Check tenant exists before processing
3. **Tier-based features:** Premium features require both valid tenant AND premium plan
4. **Cleanup in finally:** Always clear ThreadLocal to prevent memory leaks
5. **JSON-based routing:** Tenant info in payload (REST best practice for body-centric APIs)

---

## Example 3: Feature Flag System

**Scenario:** Declarative feature flags using AOP annotations.

### Implementation

```java
package com.example.features;

import com.cleveloper.jufu.requestutils.condition.aop.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller demonstrating feature flag patterns.
 */
@RestController
@RequestMapping("/api/features")
public class FeatureController {

    /**
     * Beta feature - requires ?beta=true query parameter.
     */
    @GetMapping("/beta")
    @JUFUMatchConditions(
        queryParams = @JUFUQueryParamCondition(
            name = "beta",
            value = "true",
            ignoreCase = true
        )
    )
    public ResponseEntity<?> betaFeature() {
        return ResponseEntity.ok(Map.of(
            "feature", "beta",
            "status", "enabled",
            "message", "You're using the new beta UI!"
        ));
    }

    /**
     * Admin-only feature - requires X-User-Role: admin header.
     */
    @GetMapping("/admin")
    @JUFUMatchConditions(
        headers = @JUFUHeaderCondition(
            name = "X-User-Role",
            value = "admin"
        )
    )
    public ResponseEntity<?> adminFeature() {
        return ResponseEntity.ok(Map.of(
            "feature", "admin-panel",
            "data", "Sensitive admin data"
        ));
    }

    /**
     * A/B test feature - users in group A or B.
     */
    @GetMapping("/ab-test")
    @JUFUMatchConditions(
        mode = LogicMode.OR,
        queryParams = {
            @JUFUQueryParamCondition(name = "group", value = "A"),
            @JUFUQueryParamCondition(name = "group", value = "B")
        }
    )
    public ResponseEntity<?> abTestFeature(@RequestParam String group) {
        return ResponseEntity.ok(Map.of(
            "feature", "ab-test",
            "group", group,
            "variant", group.equals("A") ? "variant-A" : "variant-B"
        ));
    }

    /**
     * Mobile-only feature.
     */
    @GetMapping("/mobile-exclusive")
    @JUFUMatchConditions(
        headers = @JUFUHeaderCondition(
            name = "User-Agent",
            value = ".*Mobile.*",
            operation = MatchOperation.REGEX,
            ignoreCase = true
        )
    )
    public ResponseEntity<?> mobileFeature() {
        return ResponseEntity.ok(Map.of(
            "feature", "mobile-exclusive",
            "message", "Mobile-optimized feature"
        ));
    }
}
```

**Exception Handler:**

```java
package com.example.features;

import com.cleveloper.jufu.requestutils.condition.exceptions.ConditionNotMetException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class FeatureExceptionHandler {

    @ExceptionHandler(ConditionNotMetException.class)
    public ResponseEntity<?> handleFeatureNotAvailable(ConditionNotMetException ex) {
        return ResponseEntity.status(403).body(Map.of(
            "error", "Feature not available",
            "message", "This feature is not enabled for your account"
        ));
    }
}
```

---

## Example 4: Smart API Gateway

**Scenario:** Combine all features - versioning, authentication, rate limiting, tenant routing.

```java
package com.example.gateway;

import com.cleveloper.jufu.requestutils.condition.core.*;
import com.cleveloper.jufu.requestutils.condition.matchers.*;
import com.example.conditions.RateLimitCondition;
import com.example.conditions.WorkingHoursCondition;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;

/**
 * Smart API Gateway with multi-layered validation.
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private final RequestConditionMatcher matcher;

    // Layer 1: Authentication
    private static final Condition VALID_API_KEY = new HeaderCondition(
        "X-Api-Key", "sk-prod-.*", MatchOperation.REGEX, false
    );

    // Layer 2: Rate limiting
    private static final Condition RATE_LIMIT = new RateLimitCondition(
        100, Duration.ofMinutes(1)
    );

    // Layer 3: Business hours (for support endpoints)
    private static final Condition BUSINESS_HOURS = new WorkingHoursCondition(
        9, 17, ZoneId.of("America/New_York")
    );

    // Layer 4: Version check
    private static final Condition API_V2 = ConditionGroup.or(
        new HeaderCondition("X-Api-Version", "v2", MatchOperation.EQUALS, false),
        new QueryParamCondition("version", "v2", MatchOperation.EQUALS, true)
    );

    @Autowired
    public GatewayController(RequestConditionMatcher matcher) {
        this.matcher = matcher;
    }

    /**
     * Public endpoint - minimal validation.
     */
    @GetMapping("/public")
    public ResponseEntity<?> publicEndpoint(HttpServletRequest request) {
        // Only rate limiting
        ConditionResult result = matcher.evaluate(RATE_LIMIT, request);

        if (!result.isMatched()) {
            return ResponseEntity.status(429).body(Map.of(
                "error", "Too many requests"
            ));
        }

        return ResponseEntity.ok(Map.of("data", "public"));
    }

    /**
     * Authenticated endpoint - requires API key and rate limit.
     */
    @GetMapping("/authenticated")
    public ResponseEntity<?> authenticatedEndpoint(HttpServletRequest request) {
        Condition requirements = ConditionGroup.and(VALID_API_KEY, RATE_LIMIT);
        ConditionResult result = matcher.evaluate(requirements, request);

        if (!result.isMatched()) {
            return buildErrorResponse(result);
        }

        return ResponseEntity.ok(Map.of("data", "authenticated"));
    }

    /**
     * Premium endpoint - requires API key, rate limit, and v2 API.
     */
    @GetMapping("/premium")
    public ResponseEntity<?> premiumEndpoint(HttpServletRequest request) {
        Condition requirements = ConditionGroup.and(
            VALID_API_KEY,
            RATE_LIMIT,
            API_V2
        );

        ConditionResult result = matcher.evaluate(
            requirements,
            request
        );

        if (!result.isMatched()) {
            return buildErrorResponse(result);
        }

        return ResponseEntity.ok(Map.of(
            "data", "premium",
            "version", "v2"
        ));
    }

    /**
     * Support endpoint - only available during business hours.
     */
    @GetMapping("/support")
    public ResponseEntity<?> supportEndpoint(HttpServletRequest request) {
        Condition requirements = ConditionGroup.and(
            VALID_API_KEY,
            RATE_LIMIT,
            BUSINESS_HOURS
        );

        ConditionResult result = matcher.evaluate(requirements, request);

        if (!result.isMatched()) {
            return buildErrorResponse(result);
        }

        return ResponseEntity.ok(Map.of(
            "service", "support",
            "status", "available"
        ));
    }

    private ResponseEntity<?> buildErrorResponse(ConditionResult result) {
        ConditionFailure firstFailure = result.getFailures().get(0);

        int statusCode = switch (firstFailure.getConditionType()) {
            case "RateLimit" -> 429;
            case "WorkingHours" -> 503;
            case "Header" -> 401;
            default -> 403;
        };

        return ResponseEntity.status(statusCode).body(Map.of(
            "error", firstFailure.getMessage(),
            "failures", result.getFailures().size()
        ));
    }
}
```

### Design Decisions

1. **Layered validation:** Authentication → Rate limit → Business rules → Version
2. **Fail fast by default:** Stop at first failure for performance
3. **Reusable conditions:** Define once, combine differently per endpoint
4. **Specific error codes:** 401 (auth), 429 (rate limit), 503 (unavailable)
5. **Progressive enhancement:** Public → Authenticated → Premium tiers

---

## Key Takeaways

1. **Start simple:** Version routing is easiest entry point
2. **Compose conditions:** Build complex validation from simple building blocks
3. **Separate concerns:** DTOs, services, and controllers have distinct responsibilities
4. **Test thoroughly:** Unit and integration tests for all paths
5. **Document decisions:** Explain why you chose specific patterns
6. **Use constants:** Pre-define conditions for reuse
7. **Handle failures gracefully:** Provide actionable error messages

---

## Next Steps

**Having issues?**
→ [Troubleshooting](09-troubleshooting.md) - Debug common problems

**Want to learn more?**
→ [Back to Index](00-index.md) - Review specific topics

---

**[← Custom Conditions](07-custom-conditions.md)** | **[Back to Index](00-index.md)** | **[Next: Troubleshooting →](09-troubleshooting.md)**
