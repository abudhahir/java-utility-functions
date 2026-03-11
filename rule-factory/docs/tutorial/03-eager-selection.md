# Eager Selection

**Goal:** Master all patterns for value-based two-outcome selection with `PredicateResultFactory.select`.

---

## What is Eager Selection?

"Eager" means both candidate values are **evaluated before the factory is called**. The factory receives two already-constructed values and returns the correct one.

```java
// Both "ADULT" and "MINOR" are already String literals here
String tier = PredicateResultFactory.select(age -> age >= 18, 21, "ADULT", "MINOR");
```

---

## Pattern 1: Primitive and String Candidates

The simplest and most common use case — selecting from two scalar values.

```java
// Strings
String label = PredicateResultFactory.select(
    score -> score >= 60,
    score,
    "PASS",
    "FAIL"
);

// Integers
int fee = PredicateResultFactory.select(
    user -> user.isPremium(),
    currentUser,
    0,      // premium: free
    500     // basic: 500 cents
);

// Enums
UserTier tier = PredicateResultFactory.select(
    user -> user.getPoints() >= 1000,
    currentUser,
    UserTier.GOLD,
    UserTier.SILVER
);
```

---

## Pattern 2: Object Candidates

Select between two pre-built objects — configuration, policy objects, response templates:

```java
// Select between two API configurations
ApiConfig config = PredicateResultFactory.select(
    env -> env.isProduction(),
    environment,
    prodConfig,
    stagingConfig
);

// Select between response formats
ResponseTemplate template = PredicateResultFactory.select(
    req -> req.acceptsJson(),
    request,
    jsonTemplate,
    xmlTemplate
);
```

---

## Pattern 3: Method Reference Predicate

Use method references for cleaner, more readable code:

```java
// Instance method reference
String result = PredicateResultFactory.select(
    String::isBlank,
    inputText,
    "EMPTY",
    "HAS_CONTENT"
);

// Static method reference via lambda (static methods need wrapping)
String direction = PredicateResultFactory.select(
    n -> Integer.signum(n) >= 0,
    number,
    "NON_NEGATIVE",
    "NEGATIVE"
);
```

---

## Pattern 4: Named Predicate Constants

Extract predicates to named constants for reuse and readability:

```java
public class UserPredicates {
    public static final PredicateCondition<User> IS_PREMIUM  = User::isPremium;
    public static final PredicateCondition<User> IS_ACTIVE   = u -> u.getStatus() == Status.ACTIVE;
    public static final PredicateCondition<User> IS_ADMIN    = u -> u.getRoles().contains("ADMIN");
}

// Usage — reads almost like prose
String greeting = PredicateResultFactory.select(
    UserPredicates.IS_PREMIUM, currentUser, "VIP access granted", "Standard access"
);

String dashboardView = PredicateResultFactory.select(
    UserPredicates.IS_ADMIN, currentUser, "admin-dashboard", "user-dashboard"
);
```

---

## Pattern 5: Chained/Composed Predicates

Compose predicates with `and`, `or`, `negate` before passing to the factory:

```java
PredicateCondition<User> isPremium = User::isPremium;
PredicateCondition<User> isActive  = u -> u.getStatus() == Status.ACTIVE;

// Active AND premium
String msg = PredicateResultFactory.select(
    isPremium.and(isActive),
    user,
    "Welcome, active VIP!",
    "Account restricted"
);

// Premium OR admin
String access = PredicateResultFactory.select(
    isPremium.or(UserPredicates.IS_ADMIN),
    user,
    "FULL_ACCESS",
    "LIMITED_ACCESS"
);

// NOT premium
String price = PredicateResultFactory.select(
    isPremium.negate(),
    user,
    "STANDARD_PRICE",
    "DISCOUNTED_PRICE"
);
```

> **Note:** `and`, `or`, and `negate` are default methods inherited from `Predicate<T>` and work directly on `PredicateCondition<T>`.

---

## Pattern 6: Null Candidates

When your domain allows `null` as a valid return value, the factory handles it transparently:

```java
// whenTrue is null — returned as-is when predicate is true
String optionalLabel = PredicateResultFactory.select(
    item -> item.isArchived(),
    item,
    null,         // no label for archived items
    "ACTIVE"
);

// Both null — valid, just returns null
String noneCase = PredicateResultFactory.select(
    x -> x > 0,
    -1,
    null,
    null
);
// noneCase == null
```

---

## Pattern 7: Subtype Candidates

Use covariance to return a subtype while keeping the declared return type as the supertype:

```java
// Select between two concrete strategies but return as the interface
NotificationStrategy strategy = PredicateResultFactory.select(
    user -> user.prefersEmail(),
    currentUser,
    new EmailNotificationStrategy(),   // implements NotificationStrategy
    new SmsNotificationStrategy()      // implements NotificationStrategy
);
strategy.send(notification);
```

---

## Anti-Patterns to Avoid

### ❌ Eager with expensive construction

```java
// Both DTOs are built even if only one will be returned — wasteful
UserDto dto = PredicateResultFactory.select(
    user -> user.isPremium(),
    user,
    buildFullDto(user),     // expensive — always called
    buildBasicDto(user)     // expensive — always called
);
```

**Fix:** Use `selectLazy` — see [Lazy Selection](04-lazy-selection.md).

---

### ❌ Using eager when candidates have side effects

```java
// Both audit records are written even though only one value is returned
AuditRecord record = PredicateResultFactory.select(
    req -> req.isAdmin(),
    request,
    auditLog.writeAdminAccess(req),   // side effect fires regardless
    auditLog.writeUserAccess(req)     // side effect fires regardless
);
```

**Fix:** Wrap in suppliers and use `selectLazy`.

---

## Complete Example: Role-Based Response Selection

```java
package com.example.usermanagement;

import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

public class UserResponseSelector {

    private static final PredicateCondition<User> IS_ADMIN =
        user -> user.getRoles().contains("ADMIN");

    public String selectWelcomeBanner(User user) {
        return PredicateResultFactory.select(IS_ADMIN, user,
            "Welcome to the Admin Console",
            "Welcome to the Dashboard"
        );
    }

    public String selectTheme(User user) {
        return PredicateResultFactory.select(IS_ADMIN, user,
            "theme-dark-admin",
            "theme-light-user"
        );
    }

    public int selectSessionTimeout(User user) {
        return PredicateResultFactory.select(IS_ADMIN, user,
            30,    // admins: 30-minute session
            120    // regular users: 2-hour session
        );
    }
}
```

---

**→ Continue to [Lazy Selection](04-lazy-selection.md)**

