# Lazy Selection

**Goal:** Master supplier-based deferred selection with `PredicateResultFactory.selectLazy`.

---

## What is Lazy Selection?

"Lazy" means the candidate values are **not constructed until the factory decides which branch is needed**. You provide two `Supplier<R>` instances; only the selected one is invoked — exactly once.

```java
// buildPremiumDto is only called if user.isPremium() returns true
UserDto dto = PredicateResultFactory.selectLazy(
    user -> user.isPremium(),
    currentUser,
    () -> buildPremiumDto(currentUser),  // called only if true
    () -> buildBasicDto(currentUser)     // called only if false
);
```

---

## When to Use Lazy vs Eager

| Situation | Use |
|-----------|-----|
| Both values are cheap (strings, enums, simple objects) | `select` (eager) |
| One or both values require expensive computation | `selectLazy` |
| Construction calls a service, DB, or external API | `selectLazy` |
| Construction has side effects (logging, auditing, events) | `selectLazy` |
| You want to defer object creation to the last possible moment | `selectLazy` |

---

## Pattern 1: Expensive Object Construction

Avoid building objects you won't use:

```java
// Without lazy — both are always built
ReportDto report = PredicateResultFactory.select(
    u -> u.hasFeature("ADVANCED_REPORT"),
    user,
    reportService.buildAdvancedReport(user),  // always called
    reportService.buildBasicReport(user)      // always called
);

// With lazy — only one is built
ReportDto report = PredicateResultFactory.selectLazy(
    u -> u.hasFeature("ADVANCED_REPORT"),
    user,
    () -> reportService.buildAdvancedReport(user),  // called only if true
    () -> reportService.buildBasicReport(user)      // called only if false
);
```

---

## Pattern 2: Service and Repository Calls

Defer database or API calls to only the needed branch:

```java
UserProfile profile = PredicateResultFactory.selectLazy(
    u -> u.isCached(),
    userId,
    () -> cache.get(userId),          // cache hit: fast path
    () -> userRepository.find(userId) // cache miss: DB call
);
```

---

## Pattern 3: Conditional Side Effects

Ensure audit logs, metrics, or notifications are only triggered for the selected branch:

```java
AuditEntry entry = PredicateResultFactory.selectLazy(
    req -> req.isPrivilegedOperation(),
    request,
    () -> {
        auditLog.recordPrivilegedAccess(request);  // side effect
        return AuditEntry.privileged(request);
    },
    () -> AuditEntry.standard(request)             // no side effect
);
```

---

## Pattern 4: Named Suppliers for Clarity

Extract supplier lambdas to variables to improve readability:

```java
Supplier<PricingRule> premiumPricing = () -> pricingService.getPremiumRules(product);
Supplier<PricingRule> standardPricing = () -> pricingService.getStandardRules(product);

PricingRule rule = PredicateResultFactory.selectLazy(
    user -> user.isPremium(),
    currentUser,
    premiumPricing,
    standardPricing
);
```

---

## Pattern 5: Null Unselected Supplier

The unselected supplier may be `null` without causing failure. This is useful when one branch is a known no-op:

```java
// When predicate is true, true supplier is selected → false supplier (null) is never reached
Optional<Badge> badge = PredicateResultFactory.selectLazy(
    user -> user.isEligibleForBadge(),
    currentUser,
    () -> Optional.of(badgeService.awardBadge(currentUser)),
    null  // not eligible: no badge, unselected supplier is tolerated
);
```

> **Important:** Only the *selected* supplier reference is validated. If the selected supplier is `null`, an `IllegalArgumentException` is thrown.

```java
// predicate is true → true supplier selected → it's null → FAILS
PredicateResultFactory.selectLazy(
    u -> u.isPremium(), user,
    null,               // selected and null → IllegalArgumentException
    () -> "BASIC"
);
```

---

## Pattern 6: Supplier Returning Null

If the selected supplier exists but returns `null`, the result is `null` — no exception:

```java
// Feature flag is off — featureConfig returns null
FeatureConfig config = PredicateResultFactory.selectLazy(
    env -> env.isFeatureEnabled("BETA"),
    environment,
    () -> featureService.getBetaConfig(),  // may return null
    () -> FeatureConfig.defaults()
);
// config may be null if getBetaConfig() returns null — handle at call site
```

---

## Pattern 7: Exactly-Once Guarantee

The selected supplier is invoked exactly once per factory call — no retries, no caching, no double invocation:

```java
// Safe for suppliers with side effects — the operation fires exactly once
Order confirmedOrder = PredicateResultFactory.selectLazy(
    payment -> payment.isApproved(),
    paymentResult,
    () -> orderService.confirmOrder(paymentResult),  // fires exactly once
    () -> orderService.cancelOrder(paymentResult)    // not reached
);
```

---

## Anti-Patterns to Avoid

### ❌ Sharing mutable state in suppliers

```java
List<String> log = new ArrayList<>();

String result = PredicateResultFactory.selectLazy(
    v -> v > 0,
    value,
    () -> { log.add("true branch"); return "T"; },
    () -> { log.add("false branch"); return "F"; }
);
// Works, but mutating shared state in suppliers is fragile and hard to test
```

**Fix:** Return results from suppliers; handle side effects outside the factory or use dedicated logging.

---

### ❌ Throwing checked exceptions from suppliers

```java
// Won't compile — Supplier.get() doesn't declare checked exceptions
UserDto dto = PredicateResultFactory.selectLazy(
    u -> u.isPremium(), user,
    () -> userService.fetchPremiumData(user),  // throws IOException — compile error
    () -> UserDto.basic(user)
);
```

**Fix:** Wrap checked exceptions in runtime exceptions inside the supplier:

```java
UserDto dto = PredicateResultFactory.selectLazy(
    u -> u.isPremium(), user,
    () -> {
        try {
            return userService.fetchPremiumData(user);
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch premium data", e);
        }
    },
    () -> UserDto.basic(user)
);
```

---

## Complete Example: Tiered Report Builder

```java
package com.example.reports;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

public class ReportFactory {

    private final ReportRepository repo;
    private final ReportRenderer renderer;

    public ReportFactory(ReportRepository repo, ReportRenderer renderer) {
        this.repo = repo;
        this.renderer = renderer;
    }

    private static final PredicateCondition<User> HAS_FULL_ACCESS =
        user -> user.getRole() == Role.ANALYST || user.getRole() == Role.ADMIN;

    /**
     * Builds the appropriate report for the user's access tier.
     * Only one report type is loaded from the database.
     */
    public Report buildReport(User user, String reportId) {
        return PredicateResultFactory.selectLazy(
            HAS_FULL_ACCESS,
            user,
            () -> repo.loadFullReport(reportId),   // DB call — only if analyst/admin
            () -> repo.loadSummaryReport(reportId) // DB call — only if standard user
        );
    }

    /**
     * Renders the appropriate template.
     * Only the needed template is loaded from disk.
     */
    public String renderTemplate(User user) {
        return PredicateResultFactory.selectLazy(
            HAS_FULL_ACCESS,
            user,
            () -> renderer.loadTemplate("report-full.html"),
            () -> renderer.loadTemplate("report-summary.html")
        );
    }
}
```

---

**→ Continue to [Custom Predicates](05-custom-predicates.md)**

