# Complete Examples

**Goal:** See `rule-factory` in action with three complete, production-ready scenarios.

Each example includes full code, tests, and design decisions you can copy and adapt.

---

## Example 1: User Tier Content Router

**Scenario:** An API controller that returns different content based on user subscription tier.

### Requirements

- Premium users get full content with analytics
- Basic users get summarised content only
- Admin users see an admin dashboard view
- Predicate rules defined once, reused across methods

### Implementation

**User.java**
```java
package com.example.content;

public record User(String id, String name, boolean premium, boolean admin) {
    public boolean isPremium() { return premium; }
    public boolean isAdmin()   { return admin; }
}
```

**UserRules.java**
```java
package com.example.content;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;

public final class UserRules {
    private UserRules() {}

    public static final PredicateCondition<User> IS_PREMIUM = User::isPremium;
    public static final PredicateCondition<User> IS_ADMIN   = User::isAdmin;
}
```

**ContentRouter.java**
```java
package com.example.content;

import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

public class ContentRouter {

    private final ContentService contentService;
    private final AnalyticsService analyticsService;

    public ContentRouter(ContentService contentService, AnalyticsService analyticsService) {
        this.contentService = contentService;
        this.analyticsService = analyticsService;
    }

    /** Select full or summary article — only the needed one is loaded. */
    public Article getArticle(User user, String articleId) {
        return PredicateResultFactory.selectLazy(
            UserRules.IS_PREMIUM,
            user,
            () -> contentService.loadFullArticle(articleId),
            () -> contentService.loadSummary(articleId)
        );
    }

    /** Select dashboard view — eager, both are simple string constants. */
    public String getDashboardView(User user) {
        return PredicateResultFactory.select(
            UserRules.IS_ADMIN, user,
            "views/admin-dashboard",
            "views/user-dashboard"
        );
    }

    /** Analytics level — only record if premium. */
    public AnalyticsLevel getAnalyticsLevel(User user) {
        return PredicateResultFactory.select(
            UserRules.IS_PREMIUM, user,
            AnalyticsLevel.DETAILED,
            AnalyticsLevel.BASIC
        );
    }
}
```

### Tests

```java
package com.example.content;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentRouterTest {

    private final ContentService  contentService  = mock(ContentService.class);
    private final AnalyticsService analyticsService = mock(AnalyticsService.class);
    private final ContentRouter   router          = new ContentRouter(contentService, analyticsService);

    private final User premiumUser = new User("u1", "Alice", true,  false);
    private final User basicUser   = new User("u2", "Bob",   false, false);
    private final User adminUser   = new User("u3", "Carol", false, true);

    @Test
    void premiumUserGetsFullArticle() {
        Article full = new Article("full content");
        when(contentService.loadFullArticle("a1")).thenReturn(full);

        Article result = router.getArticle(premiumUser, "a1");

        assertEquals(full, result);
        verify(contentService).loadFullArticle("a1");
        verify(contentService, never()).loadSummary(any());
    }

    @Test
    void basicUserGetsSummaryArticle() {
        Article summary = new Article("summary");
        when(contentService.loadSummary("a1")).thenReturn(summary);

        Article result = router.getArticle(basicUser, "a1");

        assertEquals(summary, result);
        verify(contentService).loadSummary("a1");
        verify(contentService, never()).loadFullArticle(any());
    }

    @Test
    void adminUserGetsAdminDashboard() {
        assertEquals("views/admin-dashboard", router.getDashboardView(adminUser));
    }

    @Test
    void regularUserGetsUserDashboard() {
        assertEquals("views/user-dashboard", router.getDashboardView(basicUser));
    }

    @Test
    void premiumUserGetsDetailedAnalytics() {
        assertEquals(AnalyticsLevel.DETAILED, router.getAnalyticsLevel(premiumUser));
    }
}
```

---

## Example 2: Order Pricing Engine

**Scenario:** A pricing service that selects pricing strategies based on order characteristics.

### Requirements

- Bulk orders (qty ≥ 100) get discounted pricing
- Returning customers (> 5 past orders) get loyalty pricing
- Either condition qualifies — use OR composition
- Strategy construction is expensive; use lazy loading

### Implementation

**Order.java**
```java
package com.example.pricing;

public record Order(String id, Customer customer, int quantity, double subtotal) {}
```

**Customer.java**
```java
package com.example.pricing;

public record Customer(String id, int pastOrderCount) {
    public boolean isReturning() { return pastOrderCount > 5; }
}
```

**PricingRules.java**
```java
package com.example.pricing;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;

public final class PricingRules {
    private PricingRules() {}

    public static final PredicateCondition<Order> IS_BULK_ORDER =
        order -> order.quantity() >= 100;

    public static final PredicateCondition<Order> IS_RETURNING_CUSTOMER =
        order -> order.customer().isReturning();

    /** Either bulk OR returning customer qualifies for discount. */
    public static final PredicateCondition<Order> IS_DISCOUNTABLE =
        IS_BULK_ORDER.or(IS_RETURNING_CUSTOMER)::test;
}
```

**PricingEngine.java**
```java
package com.example.pricing;

import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

public class PricingEngine {

    private final PricingStrategyFactory strategyFactory;

    public PricingEngine(PricingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    /**
     * Selects and applies the correct pricing strategy.
     * Only the applicable strategy is constructed.
     */
    public PricedOrder price(Order order) {
        PricingStrategy strategy = PredicateResultFactory.selectLazy(
            PricingRules.IS_DISCOUNTABLE,
            order,
            () -> strategyFactory.createDiscountStrategy(order),  // heavy object
            () -> strategyFactory.createStandardStrategy(order)   // heavy object
        );
        return strategy.apply(order);
    }

    /**
     * Returns the display label for the applied pricing tier.
     */
    public String getPricingLabel(Order order) {
        return PredicateResultFactory.select(
            PricingRules.IS_DISCOUNTABLE,
            order,
            "DISCOUNTED",
            "STANDARD"
        );
    }
}
```

### Tests

```java
package com.example.pricing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PricingEngineTest {

    private final PricingStrategyFactory factory = mock(PricingStrategyFactory.class);
    private final PricingEngine engine = new PricingEngine(factory);

    @Test
    void bulkOrderUsesDiscountStrategy() {
        Order order = new Order("o1", new Customer("c1", 1), 200, 5000.0);
        PricingStrategy discount = mock(PricingStrategy.class);
        PricedOrder priced = new PricedOrder(order, 4000.0);
        when(factory.createDiscountStrategy(order)).thenReturn(discount);
        when(discount.apply(order)).thenReturn(priced);

        PricedOrder result = engine.price(order);

        assertEquals(priced, result);
        verify(factory).createDiscountStrategy(order);
        verify(factory, never()).createStandardStrategy(any());
    }

    @Test
    void returningCustomerUsesDiscountStrategy() {
        Order order = new Order("o2", new Customer("c2", 10), 5, 100.0);
        PricingStrategy discount = mock(PricingStrategy.class);
        PricedOrder priced = new PricedOrder(order, 90.0);
        when(factory.createDiscountStrategy(order)).thenReturn(discount);
        when(discount.apply(order)).thenReturn(priced);

        PricedOrder result = engine.price(order);

        assertEquals(priced, result);
        verify(factory).createDiscountStrategy(order);
    }

    @Test
    void newSmallOrderUsesStandardStrategy() {
        Order order = new Order("o3", new Customer("c3", 1), 5, 50.0);
        PricingStrategy standard = mock(PricingStrategy.class);
        PricedOrder priced = new PricedOrder(order, 50.0);
        when(factory.createStandardStrategy(order)).thenReturn(standard);
        when(standard.apply(order)).thenReturn(priced);

        PricedOrder result = engine.price(order);

        assertEquals(priced, result);
        verify(factory).createStandardStrategy(order);
        verify(factory, never()).createDiscountStrategy(any());
    }

    @Test
    void pricingLabelIsDiscountedForBulkOrder() {
        Order order = new Order("o4", new Customer("c4", 1), 150, 3000.0);
        assertEquals("DISCOUNTED", engine.getPricingLabel(order));
    }
}
```

---

## Example 3: Feature Flag Response Selector

**Scenario:** A feature flag system that selects API response payloads based on flag state.

### Requirements

- Feature flags stored in a `FeatureFlagService`
- Flag-on returns new response shape; flag-off returns legacy shape
- Both shapes require DB calls — use lazy selection
- Flag checks are injected as predicate rules

### Implementation

**FeatureContext.java**
```java
package com.example.flags;

public record FeatureContext(String userId, String featureKey) {}
```

**FeatureFlagRules.java**
```java
package com.example.flags;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;

public final class FeatureFlagRules {
    private FeatureFlagRules() {}

    /** Builds a rule that checks a named feature flag. */
    public static PredicateCondition<FeatureContext> flagEnabled(FeatureFlagService flags) {
        return ctx -> flags.isEnabled(ctx.featureKey(), ctx.userId());
    }
}
```

**UserApiResponseSelector.java**
```java
package com.example.flags;

import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

public class UserApiResponseSelector {

    private final UserRepository  userRepo;
    private final FeatureFlagService flags;

    public UserApiResponseSelector(UserRepository userRepo, FeatureFlagService flags) {
        this.userRepo = userRepo;
        this.flags    = flags;
    }

    /**
     * Returns either the new or legacy user response shape based on a feature flag.
     * Only one response format is loaded from the DB.
     */
    public Object getUserResponse(String userId) {
        FeatureContext ctx = new FeatureContext(userId, "new-user-api-v2");

        return PredicateResultFactory.selectLazy(
            FeatureFlagRules.flagEnabled(flags),
            ctx,
            () -> userRepo.loadNewApiResponse(userId),    // new format — DB call
            () -> userRepo.loadLegacyApiResponse(userId)  // legacy format — DB call
        );
    }
}
```

### Tests

```java
package com.example.flags;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserApiResponseSelectorTest {

    private final UserRepository     userRepo = mock(UserRepository.class);
    private final FeatureFlagService flags    = mock(FeatureFlagService.class);
    private final UserApiResponseSelector selector = new UserApiResponseSelector(userRepo, flags);

    @Test
    void flagOnReturnsNewApiResponse() {
        when(flags.isEnabled("new-user-api-v2", "u1")).thenReturn(true);
        Object newResponse = new Object();
        when(userRepo.loadNewApiResponse("u1")).thenReturn(newResponse);

        Object result = selector.getUserResponse("u1");

        assertSame(newResponse, result);
        verify(userRepo).loadNewApiResponse("u1");
        verify(userRepo, never()).loadLegacyApiResponse(any());
    }

    @Test
    void flagOffReturnsLegacyApiResponse() {
        when(flags.isEnabled("new-user-api-v2", "u2")).thenReturn(false);
        Object legacyResponse = new Object();
        when(userRepo.loadLegacyApiResponse("u2")).thenReturn(legacyResponse);

        Object result = selector.getUserResponse("u2");

        assertSame(legacyResponse, result);
        verify(userRepo).loadLegacyApiResponse("u2");
        verify(userRepo, never()).loadNewApiResponse(any());
    }
}
```

---

## Example 4: HTTP ResponseEntity Builder with Nested Predicate Selection

**Scenario:** A REST API endpoint that builds a `ResponseEntity<ApiResponse<T>>` by nesting
`PredicateResultFactory` calls. Each layer of the response — HTTP status, body, and error detail —
is resolved by its own predicate rule, producing a fully-formed, consistent JSON response without
any `if/else` blocks.

### Requirements

- Authenticated requests that pass authorisation receive `200 OK` with the requested resource
- Authenticated but unauthorised requests receive `403 Forbidden` with a structured error body
- Unauthenticated requests receive `401 Unauthorized` with a structured error body
- Malformed or resource-not-found requests receive `404 Not Found` or `422 Unprocessable Entity`
- All responses share the same `ApiResponse<T>` envelope
- Nesting resolves the outermost condition first, then delegates inner selection to a nested factory call

### Design Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Eager vs Lazy | `selectLazy` throughout | Avoids building unused response bodies and status objects |
| Nesting depth | Two levels | Outer = authentication gate; inner = authorisation / domain gate |
| Envelope type | `ApiResponse<T>` record | Consistent JSON regardless of success or failure |
| HttpStatus selection | Eager `select` on a single boolean | Status values are lightweight constants |

### File Structure

```
src/main/java/com/example/api/
├── ApiResponse.java                   # Shared response envelope
├── ApiError.java                      # Error detail inside envelope
├── RequestContext.java                # Auth + domain context per request
├── predicate/
│   └── RequestRules.java              # All predicate constants
└── controller/
    └── UserResourceController.java    # Endpoint using nested factory

src/test/java/com/example/api/controller/
└── UserResourceControllerTest.java
```

### Implementation

**ApiResponse.java**
```java
package com.example.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform JSON envelope for every API response.
 *
 * <pre>
 * // Success
 * { "success": true,  "data": { ... },  "error": null }
 *
 * // Failure
 * { "success": false, "data": null, "error": { "code": "FORBIDDEN", "message": "..." } }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    /** Factory method — success response. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Factory method — error response. */
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }
}
```

**ApiError.java**
```java
package com.example.api;

/** Error detail embedded inside {@link ApiResponse} for failure responses. */
public record ApiError(String code, String message) {}
```

**RequestContext.java**
```java
package com.example.api;

/**
 * Per-request context carrying authentication and authorisation state,
 * plus the resolved domain object (if any).
 */
public record RequestContext(
        boolean authenticated,
        boolean authorised,
        boolean resourceFound,
        boolean payloadValid,
        Object  resource        // may be null if not found / not authorised
) {}
```

**RequestRules.java**
```java
package com.example.api.predicate;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;
import com.example.api.RequestContext;

/** Predicate constants for HTTP request gate decisions. */
public final class RequestRules {

    private RequestRules() {}

    /** True when the caller has presented valid credentials. */
    public static final PredicateCondition<RequestContext> IS_AUTHENTICATED =
            RequestContext::authenticated;

    /** True when the authenticated caller has permission for the requested resource. */
    public static final PredicateCondition<RequestContext> IS_AUTHORISED =
            RequestContext::authorised;

    /** True when the requested resource exists. */
    public static final PredicateCondition<RequestContext> RESOURCE_EXISTS =
            RequestContext::resourceFound;

    /** True when the request payload passes domain validation. */
    public static final PredicateCondition<RequestContext> PAYLOAD_VALID =
            RequestContext::payloadValid;
}
```

**UserResourceController.java**
```java
package com.example.api.controller;

import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;
import com.example.api.ApiResponse;
import com.example.api.RequestContext;
import com.example.api.predicate.RequestRules;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Demonstrates nested {@link PredicateResultFactory} calls to build a
 * {@link ResponseEntity} with a typed {@link ApiResponse} envelope — no if/else.
 *
 * <p>Decision tree:
 * <pre>
 *   authenticated?
 *   ├── NO  → 401 Unauthorized
 *   └── YES → authorised?
 *               ├── NO  → 403 Forbidden
 *               └── YES → resource exists?
 *                           ├── NO  → 404 Not Found
 *                           └── YES → payload valid?
 *                                       ├── NO  → 422 Unprocessable Entity
 *                                       └── YES → 200 OK  (resource body)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserResourceController {

    private final UserService userService;

    public UserResourceController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/users/{id}
     *
     * <p>Resolves the response by nesting four predicate factory calls.
     * Only the branch that is actually reached is constructed.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        RequestContext ctx = userService.buildContext(id, authHeader);

        return PredicateResultFactory.selectLazy(

            // ── Gate 1: Authentication ───────────────────────────────────────
            RequestRules.IS_AUTHENTICATED,
            ctx,

            // Authenticated branch → proceed to inner gates
            () -> PredicateResultFactory.selectLazy(

                // ── Gate 2: Authorisation ────────────────────────────────────
                RequestRules.IS_AUTHORISED,
                ctx,

                // Authorised branch → proceed to resource check
                () -> PredicateResultFactory.selectLazy(

                    // ── Gate 3: Resource existence ───────────────────────────
                    RequestRules.RESOURCE_EXISTS,
                    ctx,

                    // Resource found → proceed to payload validation
                    () -> PredicateResultFactory.selectLazy(

                        // ── Gate 4: Payload / domain validity ────────────────
                        RequestRules.PAYLOAD_VALID,
                        ctx,

                        // ✅ All gates passed — 200 OK
                        () -> ResponseEntity
                                .status(HttpStatus.OK)
                                .body(ApiResponse.ok(
                                        UserDto.from(ctx.resource())
                                )),

                        // ❌ Invalid payload — 422 Unprocessable Entity
                        () -> ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .<ApiResponse<UserDto>>body(ApiResponse.error(
                                        "INVALID_INPUT",
                                        "Request payload did not pass domain validation"
                                ))
                    ),

                    // ❌ Resource not found — 404 Not Found
                    () -> ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .<ApiResponse<UserDto>>body(ApiResponse.error(
                                    "USER_NOT_FOUND",
                                    "No user found with id: " + id
                            ))
                ),

                // ❌ Not authorised — 403 Forbidden
                () -> ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .<ApiResponse<UserDto>>body(ApiResponse.error(
                                "FORBIDDEN",
                                "You do not have permission to access this resource"
                        ))
            ),

            // ❌ Not authenticated — 401 Unauthorized
            () -> ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .<ApiResponse<UserDto>>body(ApiResponse.error(
                            "UNAUTHORIZED",
                            "Valid authentication credentials are required"
                    ))
        );
    }
}
```

> **Reading the nesting:** Each `selectLazy` call is a single decision gate.
> The true-branch supplier either returns a final `ResponseEntity` or delegates
> to the next inner gate via another `selectLazy` call.
> The false-branch supplier always returns a final `ResponseEntity` immediately.
> This means each level resolves exactly one HTTP status category.

### Supporting Types

**UserDto.java**
```java
package com.example.api.controller;

/** Outbound user representation. */
public record UserDto(String id, String name, String email) {
    public static UserDto from(Object resource) {
        // Cast to your actual domain User type in a real application
        User user = (User) resource;
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }
}
```

**UserService.java**
```java
package com.example.api.controller;

import com.example.api.RequestContext;

public interface UserService {
    /**
     * Resolves authentication, authorisation, and resource lookup
     * into a single {@link RequestContext} for downstream gate evaluation.
     */
    RequestContext buildContext(String userId, String authHeader);
}
```

### JSON Responses

**200 OK — success**
```json
{
  "success": true,
  "data": {
    "id": "u-101",
    "name": "Alice Smith",
    "email": "alice@example.com"
  }
}
```

**401 Unauthorized**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Valid authentication credentials are required"
  }
}
```

**403 Forbidden**
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "You do not have permission to access this resource"
  }
}
```

**404 Not Found**
```json
{
  "success": false,
  "error": {
    "code": "USER_NOT_FOUND",
    "message": "No user found with id: u-999"
  }
}
```

**422 Unprocessable Entity**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "Request payload did not pass domain validation"
  }
}
```

### Tests

```java
package com.example.api.controller;

import com.example.api.ApiResponse;
import com.example.api.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserResourceControllerTest {

    private final UserService            service    = mock(UserService.class);
    private final UserResourceController controller = new UserResourceController(service);

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static RequestContext ctx(
            boolean auth, boolean authz, boolean found, boolean valid, Object resource) {
        return new RequestContext(auth, authz, found, valid, resource);
    }

    private static final Object RESOURCE = new Object();

    // ── 200 OK ──────────────────────────────────────────────────────────────

    @Test
    void allGatesPassReturns200WithData() {
        when(service.buildContext("u1", "Bearer token")).thenReturn(
                ctx(true, true, true, true, RESOURCE));

        ResponseEntity<ApiResponse<UserDto>> response =
                controller.getUser("u1", "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertNotNull(response.getBody().data());
        assertNull(response.getBody().error());
    }

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @Test
    void notAuthenticatedReturns401() {
        when(service.buildContext("u1", null)).thenReturn(
                ctx(false, false, false, false, null));

        ResponseEntity<ApiResponse<UserDto>> response = controller.getUser("u1", null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("UNAUTHORIZED", response.getBody().error().code());
        assertNull(response.getBody().data());
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @Test
    void authenticatedButNotAuthorisedReturns403() {
        when(service.buildContext("u2", "Bearer token")).thenReturn(
                ctx(true, false, false, false, null));

        ResponseEntity<ApiResponse<UserDto>> response =
                controller.getUser("u2", "Bearer token");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("FORBIDDEN", response.getBody().error().code());
    }

    // ── 404 Not Found ───────────────────────────────────────────────────────

    @Test
    void authorisedButResourceNotFoundReturns404() {
        when(service.buildContext("u999", "Bearer token")).thenReturn(
                ctx(true, true, false, false, null));

        ResponseEntity<ApiResponse<UserDto>> response =
                controller.getUser("u999", "Bearer token");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("USER_NOT_FOUND", response.getBody().error().code());
        assertTrue(response.getBody().error().message().contains("u999"));
    }

    // ── 422 Unprocessable Entity ─────────────────────────────────────────────

    @Test
    void invalidPayloadReturns422() {
        when(service.buildContext("u1", "Bearer token")).thenReturn(
                ctx(true, true, true, false, RESOURCE));

        ResponseEntity<ApiResponse<UserDto>> response =
                controller.getUser("u1", "Bearer token");

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("INVALID_INPUT", response.getBody().error().code());
    }

    // ── Lazy isolation: only the reached branch is evaluated ─────────────────

    @Test
    void unauthenticatedRequestNeverEvaluatesInnerGates() {
        // RequestContext with auth=false; inner state is intentionally "corrupt"
        // to prove inner gates are never reached
        RequestContext unauthCtx = ctx(false, true, true, true, RESOURCE);
        when(service.buildContext("u1", null)).thenReturn(unauthCtx);

        ResponseEntity<ApiResponse<UserDto>> response = controller.getUser("u1", null);

        // Despite inner flags being "true", outer gate short-circuits at 401
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
```

---

**→ Continue to [Troubleshooting](07-troubleshooting.md)**



