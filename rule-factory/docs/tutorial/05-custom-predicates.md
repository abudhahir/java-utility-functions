# Custom Predicates

**Goal:** Build and use domain-specific predicate interfaces that extend `PredicateCondition<T>` for readable, reusable business rules.

---

## Why Custom Predicate Interfaces?

The factory accepts any `Predicate<T>` extension, including your own domain-specific interfaces. Custom interfaces let you:

- Give predicates meaningful domain names
- Group related predicates as constants on a dedicated class
- Pass predicates as named parameters in APIs
- Write tests against a clearly typed contract
- Compose predicates with `and`, `or`, `negate` without losing domain type

---

## Defining a Custom Predicate Interface

Extend `PredicateCondition<T>` (which itself extends `Predicate<T>`):

```java
package com.example.domain.predicate;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;

@FunctionalInterface
public interface UserRule extends PredicateCondition<User> {
    // No additional methods needed — it's a functional interface
    // Just a domain-named predicate for User objects
}
```

Or extend `Predicate<T>` directly — both work:

```java
@FunctionalInterface
public interface OrderRule extends java.util.function.Predicate<Order> {
}
```

> Both approaches compile and are accepted by the factory. Extending `PredicateCondition<T>` adds minor clarity that the interface is intended for factory use.

---

## Pattern 1: Predicate Constants on a Domain Class

Collect related predicates as named constants:

```java
package com.example.domain.predicate;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;
import com.example.domain.User;
import com.example.domain.Status;

public final class UserRules {

    private UserRules() {}

    public static final PredicateCondition<User> IS_PREMIUM =
        User::isPremium;

    public static final PredicateCondition<User> IS_ACTIVE =
        user -> user.getStatus() == Status.ACTIVE;

    public static final PredicateCondition<User> IS_ADMIN =
        user -> user.getRoles().contains("ADMIN");

    public static final PredicateCondition<User> IS_VERIFIED =
        user -> user.isEmailVerified() && user.isPhoneVerified();

    // Composed from simpler rules
    public static final PredicateCondition<User> IS_ELIGIBLE_FOR_UPGRADE =
        IS_ACTIVE.and(IS_VERIFIED).and(IS_PREMIUM.negate())::test;
}
```

Usage reads naturally:

```java
String tier = PredicateResultFactory.select(
    UserRules.IS_PREMIUM, user, "PREMIUM", "BASIC"
);

String dashboard = PredicateResultFactory.select(
    UserRules.IS_ADMIN, user, "admin-view", "user-view"
);
```

---

## Pattern 2: Parameterized Predicate Factories

Create predicates that capture parameters for flexible reuse:

```java
public final class OrderRules {

    private OrderRules() {}

    /** Returns a rule that checks if order total exceeds the given threshold. */
    public static PredicateCondition<Order> totalExceeds(double threshold) {
        return order -> order.getTotal() > threshold;
    }

    /** Returns a rule that checks if order contains a specific product type. */
    public static PredicateCondition<Order> containsProductType(ProductType type) {
        return order -> order.getItems().stream()
            .anyMatch(item -> item.getProductType() == type);
    }
}

// Usage
String shippingLabel = PredicateResultFactory.select(
    OrderRules.totalExceeds(100.0),
    order,
    "FREE_SHIPPING",
    "STANDARD_SHIPPING"
);

String handlingNote = PredicateResultFactory.select(
    OrderRules.containsProductType(ProductType.FRAGILE),
    order,
    "HANDLE WITH CARE",
    "STANDARD HANDLING"
);
```

---

## Pattern 3: Domain-Named Functional Interface

Give the interface a meaningful name that reflects your business language:

```java
@FunctionalInterface
public interface EligibilityCheck extends PredicateCondition<Applicant> {}

@FunctionalInterface
public interface FraudSignal extends PredicateCondition<Transaction> {}

@FunctionalInterface
public interface ContentFilter extends PredicateCondition<Post> {}
```

Use in service signatures for maximum clarity:

```java
public class LoanService {

    public LoanOffer evaluate(Applicant applicant, EligibilityCheck eligibility) {
        return PredicateResultFactory.selectLazy(
            eligibility,
            applicant,
            () -> buildPremiumOffer(applicant),
            () -> buildStandardOffer(applicant)
        );
    }
}

// Call site is self-documenting
loanService.evaluate(applicant, a -> a.getCreditScore() >= 750);
```

---

## Pattern 4: Predicate Composition

Since `PredicateCondition<T>` extends `Predicate<T>`, all composition methods are available:

```java
PredicateCondition<User> isPremium  = UserRules.IS_PREMIUM;
PredicateCondition<User> isActive   = UserRules.IS_ACTIVE;
PredicateCondition<User> isVerified = UserRules.IS_VERIFIED;

// AND
String msg = PredicateResultFactory.select(
    isPremium.and(isActive),
    user,
    "Active premium member",
    "Account restricted"
);

// OR
String access = PredicateResultFactory.select(
    isPremium.or(UserRules.IS_ADMIN),
    user,
    "ELEVATED_ACCESS",
    "STANDARD_ACCESS"
);

// NOT
String price = PredicateResultFactory.select(
    isPremium.negate(),
    user,
    "FULL_PRICE",
    "MEMBER_PRICE"
);

// Complex composition
PredicateCondition<User> eligibleForPromo = isPremium.and(isVerified).and(isActive);

String promo = PredicateResultFactory.select(
    eligibleForPromo,
    user,
    "20% discount applied",
    "No promotion available"
);
```

---

## Pattern 5: Testing Custom Predicates Independently

Custom predicate interfaces are independently testable — test the rule first, then test the factory call:

```java
// Test the rule in isolation
@Test
void isPremiumReturnsTrueForPremiumUser() {
    User premiumUser = new User("Alice", true, Status.ACTIVE);
    assertTrue(UserRules.IS_PREMIUM.test(premiumUser));
}

// Test the factory integration separately
@Test
void selectReturnsPremiumTierForPremiumUser() {
    User user = new User("Alice", true, Status.ACTIVE);
    String tier = PredicateResultFactory.select(UserRules.IS_PREMIUM, user, "PREMIUM", "BASIC");
    assertEquals("PREMIUM", tier);
}
```

---

## Pattern 6: Accepting Custom Predicates in Service APIs

Accept predicates as method parameters to make services configurable:

```java
public class NotificationRouter {

    /**
     * Routes notification to the correct channel based on the provided channel rule.
     *
     * @param user        the recipient
     * @param channelRule a predicate that returns true for primary channel preference
     * @return the selected channel
     */
    public NotificationChannel route(User user, PredicateCondition<User> channelRule) {
        return PredicateResultFactory.select(
            channelRule,
            user,
            NotificationChannel.EMAIL,
            NotificationChannel.SMS
        );
    }
}

// Configurable at call site — no hard-coded logic in the router
router.route(user, User::prefersEmail);
router.route(user, u -> u.getNotificationSettings().isPushEnabled());
```

---

## Anti-Patterns to Avoid

### ❌ Stateful predicate instances

```java
// Mutable state in predicates is a threading hazard
public class CountingPredicate implements PredicateCondition<Integer> {
    private int callCount = 0;  // ← mutable state — AVOID

    @Override
    public boolean test(Integer value) {
        callCount++;            // not thread-safe
        return value > 0;
    }
}
```

**Fix:** Keep predicates stateless. Move counters to test infrastructure (e.g., `AtomicInteger`) only when testing invocation counts.

---

### ❌ Throwing checked exceptions from predicate `test`

```java
// Won't compile — Predicate.test() doesn't declare checked exceptions
PredicateCondition<User> badRule = user -> {
    throw new IOException("cannot check");  // compile error
};
```

**Fix:** Wrap in a runtime exception:

```java
PredicateCondition<User> safeRule = user -> {
    try {
        return externalService.isEligible(user);
    } catch (IOException e) {
        throw new RuntimeException("Eligibility check failed", e);
    }
};
```

---

## Complete Example: Domain Rule Registry

```java
package com.example.pricing;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

/** Central registry of pricing rules for the checkout domain. */
public final class PricingRules {

    private PricingRules() {}

    @FunctionalInterface
    public interface PricingRule extends PredicateCondition<Order> {}

    public static final PricingRule IS_BULK_ORDER =
        order -> order.getQuantity() >= 100;

    public static final PricingRule IS_RETURNING_CUSTOMER =
        order -> order.getCustomer().getOrderCount() > 5;

    public static final PricingRule IS_DISCOUNTABLE =
        IS_BULK_ORDER.or(IS_RETURNING_CUSTOMER)::test;

    /** Selects the correct pricing strategy for the given order. */
    public static PricingStrategy selectStrategy(Order order) {
        return PredicateResultFactory.selectLazy(
            IS_DISCOUNTABLE,
            order,
            () -> new DiscountedPricingStrategy(order),
            () -> new StandardPricingStrategy(order)
        );
    }
}
```

---

**→ Continue to [Complete Examples](06-complete-examples.md)**

