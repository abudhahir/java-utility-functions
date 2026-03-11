# Quick Start

**Goal:** Make your first `PredicateResultFactory` call in 5 minutes.

This guide gets you running with minimal explanation. Want the full picture? See [Core Concepts](02-core-concepts.md) after completing this guide.

---

## What We're Building

A simple user-tier selector that returns a greeting based on whether a user is premium.

**Input:** A `User` object  
**Output:** A `String` greeting chosen by the factory

---

## Step 1: Add the Dependency

Add `rule-factory` to your `pom.xml`:

```xml
<dependency>
    <groupId>com.cleveloper.jufu</groupId>
    <artifactId>rule-factory</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

---

## Step 2: Import the Factory

```java
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;
```

---

## Step 3: Make Your First Call

```java
public class GreetingService {

    public String greet(User user) {
        return PredicateResultFactory.select(
            u -> u.isPremium(),     // predicate
            user,                   // input
            "Welcome back, VIP!",   // returned when predicate is true
            "Hello, member!"        // returned when predicate is false
        );
    }
}
```

**That's it.** No `if/else`, no null checks, one expressive line.

---

## Step 4: Test It

```java
User premium = new User("Alice", true);
User basic   = new User("Bob",   false);

System.out.println(greetingService.greet(premium));  // Welcome back, VIP!
System.out.println(greetingService.greet(basic));    // Hello, member!
```

---

## 🎉 Success!

You just used `PredicateResultFactory.select()` — the eager variant. In a few lines you've:

1. Defined a predicate inline as a lambda
2. Provided two candidate return values
3. Let the factory choose the correct one

---

## What's Next?

| Want to... | Go to... |
|-----------|---------|
| Understand how it works | [Core Concepts](02-core-concepts.md) |
| Learn all eager patterns | [Eager Selection](03-eager-selection.md) |
| Defer expensive construction | [Lazy Selection](04-lazy-selection.md) |
| Use a custom predicate interface | [Custom Predicates](05-custom-predicates.md) |

---

## Quick Reference

```java
// Eager: both values always exist; factory picks one
String result = PredicateResultFactory.select(
    predicate,    // PredicateCondition<? super T> or any Predicate<? super T> extension
    input,        // T — value passed to predicate
    whenTrue,     // R — returned if predicate evaluates to true
    whenFalse     // R — returned if predicate evaluates to false
);

// Lazy: only the selected supplier is called — exactly once
String result = PredicateResultFactory.selectLazy(
    predicate,          // same predicate contract
    input,              // T
    () -> buildTrue(),  // Supplier<R> — called only if true
    () -> buildFalse()  // Supplier<R> — called only if false
);
```

**→ Continue to [Core Concepts](02-core-concepts.md)**

