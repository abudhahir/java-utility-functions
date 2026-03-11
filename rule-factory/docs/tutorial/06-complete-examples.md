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

## Example 4: Feature-Gated Endpoint with Response Envelope

**Scenario:** A REST endpoint that gates access by subscription plan, then validates the request —
building a consistent `ApiResponse<T>` envelope for every outcome without `if/else`.

### Requirements

- Users whose plan does not include the feature receive `402 Payment Required`
- Users with access but a blank query receive `400 Bad Request`
- Valid requests receive `200 OK` with the result wrapped in `ApiResponse<T>`
- Only the reached branch builds a response body

### Design Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Eager vs Lazy | `selectLazy` throughout | Avoids constructing unused response bodies |
| Nesting depth | Two levels | Outer = plan gate; inner = input gate |
| Envelope type | `ApiResponse<T>` record | Consistent JSON shape across all outcomes |

### File Structure

```
src/main/java/com/example/feature/
├── ApiResponse.java          # Shared response envelope
├── ApiError.java             # Error detail inside envelope
├── FeatureRequest.java       # Per-request domain context
├── FeatureRules.java         # Predicate constants
├── FeatureService.java       # Service interface
└── FeatureController.java    # Endpoint using nested selectLazy

src/test/java/com/example/feature/
└── FeatureControllerTest.java
```

### Implementation

**ApiResponse.java**
```java
package com.example.feature;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform JSON envelope for every API response.
 *
 * <pre>
 * { "success": true,  "data": "result" }
 * { "success": false, "error": { "code": "INVALID_INPUT", "message": "..." } }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }
}
```

**ApiError.java**
```java
package com.example.feature;

public record ApiError(String code, String message) {}
```

**FeatureRequest.java**
```java
package com.example.feature;

public record FeatureRequest(String userId, boolean planIncludesFeature, String query) {}
```

**FeatureRules.java**
```java
package com.example.feature;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;

public final class FeatureRules {
    private FeatureRules() {}

    public static final PredicateCondition<FeatureRequest> PLAN_INCLUDES_FEATURE =
        FeatureRequest::planIncludesFeature;

    public static final PredicateCondition<FeatureRequest> QUERY_VALID =
        req -> req.query() != null && !req.query().isBlank();
}
```

**FeatureService.java**
```java
package com.example.feature;

public interface FeatureService {
    String process(FeatureRequest req);
}
```

**FeatureController.java**
```java
package com.example.feature;

import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Demonstrates two-level nested {@link PredicateResultFactory} calls to build a
 * {@link ResponseEntity} with a typed {@link ApiResponse} envelope — no if/else.
 *
 * <p>Decision tree:
 * <pre>
 *   plan includes feature?
 *   ├── NO  → 402 Payment Required
 *   └── YES → query valid?
 *               ├── NO  → 400 Bad Request
 *               └── YES → 200 OK
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/feature")
public class FeatureController {

    private final FeatureService featureService;

    public FeatureController(FeatureService featureService) {
        this.featureService = featureService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> processFeature(@RequestBody FeatureRequest req) {

        return PredicateResultFactory.selectLazy(
            FeatureRules.PLAN_INCLUDES_FEATURE, req,

            // Plan gate passed → check input validity
            () -> PredicateResultFactory.selectLazy(
                FeatureRules.QUERY_VALID, req,

                // ✅ Both gates passed — 200 OK
                () -> ResponseEntity.ok(ApiResponse.ok(featureService.process(req))),

                // ❌ Blank query — 400 Bad Request
                () -> ResponseEntity.badRequest()
                        .<ApiResponse<String>>body(ApiResponse.error(
                                "INVALID_INPUT", "query must not be blank"))
            ),

            // ❌ Feature not in plan — 402 Payment Required
            () -> ResponseEntity
                    .status(HttpStatus.PAYMENT_REQUIRED)
                    .<ApiResponse<String>>body(ApiResponse.error(
                            "PLAN_UPGRADE_REQUIRED",
                            "This feature is not included in your current plan"))
        );
    }
}
```

### JSON Responses

**200 OK**
```json
{ "success": true, "data": "result text" }
```

**400 Bad Request**
```json
{ "success": false, "error": { "code": "INVALID_INPUT", "message": "query must not be blank" } }
```

**402 Payment Required**
```json
{ "success": false, "error": { "code": "PLAN_UPGRADE_REQUIRED", "message": "This feature is not included in your current plan" } }
```

### Tests

```java
package com.example.feature;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeatureControllerTest {

    private final FeatureService    service    = mock(FeatureService.class);
    private final FeatureController controller = new FeatureController(service);

    @Test
    void validPlanAndQueryReturns200() {
        FeatureRequest req = new FeatureRequest("u1", true, "search term");
        when(service.process(req)).thenReturn("result text");

        ResponseEntity<ApiResponse<String>> response = controller.processFeature(req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().success());
        assertEquals("result text", response.getBody().data());
    }

    @Test
    void planNotIncludedReturns402() {
        FeatureRequest req = new FeatureRequest("u2", false, "search term");

        ResponseEntity<ApiResponse<String>> response = controller.processFeature(req);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("PLAN_UPGRADE_REQUIRED", response.getBody().error().code());
    }

    @Test
    void blankQueryReturns400() {
        FeatureRequest req = new FeatureRequest("u3", true, "  ");

        ResponseEntity<ApiResponse<String>> response = controller.processFeature(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("INVALID_INPUT", response.getBody().error().code());
    }

    @Test
    void planGateBlocksQueryEvaluation() {
        // Plan excluded — service must never be called even if query is valid
        FeatureRequest req = new FeatureRequest("u4", false, "valid query");

        ResponseEntity<ApiResponse<String>> response = controller.processFeature(req);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        verify(service, never()).process(any());
    }
}
```

---

**→ Continue to [Troubleshooting](07-troubleshooting.md)**



