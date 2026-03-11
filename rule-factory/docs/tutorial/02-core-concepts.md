# Core Concepts

**Goal:** Understand the architecture and key types of `rule-factory`.

This guide explains the building blocks. After reading, you'll know when to use `select` vs `selectLazy`, how the predicate contract works, and how validation and exceptions behave.

---

## Architecture Overview

The factory follows a simple evaluation flow:

```
input + predicate  →  PredicateResultFactory  →  selected value
                              ↓
                    evaluate predicate once
                    pick true or false branch
                    return branch value / invoke branch supplier
```

**Key insight:** The factory is a pure, stateless function. It holds no state, creates no objects beyond the return value, and is safe to call from any thread.

---

## The Two Operations

### `select` — Eager

```java
public static <T, R> R select(
    PredicateCondition<? super T> predicate,
    T input,
    R whenTrue,
    R whenFalse
)
```

Use `select` when **both candidate values already exist** at call time. Both values are constructed before the call — the factory simply picks one.

```java
String tier = PredicateResultFactory.select(
    user -> user.isPremium(),
    currentUser,
    "PREMIUM",
    "BASIC"
);
```

**When to use:**
- Candidates are lightweight (strings, enums, simple objects)
- Both values are already constructed
- Construction cost is not a concern

---

### `selectLazy` — Lazy

```java
public static <T, R> R selectLazy(
    PredicateCondition<? super T> predicate,
    T input,
    Supplier<? extends R> whenTrueSupplier,
    Supplier<? extends R> whenFalseSupplier
)
```

Use `selectLazy` when **candidate construction is expensive or has side effects**. Only the selected supplier is invoked — exactly once.

```java
UserDto dto = PredicateResultFactory.selectLazy(
    user -> user.isPremium(),
    currentUser,
    () -> premiumDtoBuilder.build(currentUser),  // called only if true
    () -> basicDtoBuilder.build(currentUser)     // called only if false
);
```

**When to use:**
- Candidates are expensive to build (DB calls, service calls, heavy computation)
- Construction has side effects you don't want to trigger unless needed
- One branch is significantly more expensive than the other

---

## The Predicate Contract

Both operations accept a `PredicateCondition<T>`:

```java
@FunctionalInterface
public interface PredicateCondition<T> extends java.util.function.Predicate<T> {
}
```

**Key properties:**
- It's a `@FunctionalInterface` — accept it as a lambda or method reference
- It extends `Predicate<T>` — any `Predicate` extension works
- The type bound `? super T` allows predicate reuse across subtypes

```java
// Lambda
PredicateResultFactory.select(x -> x > 0, value, "positive", "non-positive");

// Method reference
PredicateResultFactory.select(String::isBlank, text, "EMPTY", "HAS_TEXT");

// Named predicate
PredicateCondition<Integer> isAdult = age -> age >= 18;
PredicateResultFactory.select(isAdult, userAge, "ADULT", "MINOR");
```

---

## Validation Behavior

### Null predicate

If the predicate argument is `null`, both `select` and `selectLazy` throw immediately:

```java
PredicateResultFactory.select(null, input, "T", "F");
// → IllegalArgumentException: "predicate must not be null"
```

**Why `IllegalArgumentException`?** It signals a programming error at the call site — the predicate is a required argument, not an optional one.

---

### Eager mode: null candidates are allowed

In `select`, either or both candidates may be `null`. The selected value is returned as-is:

```java
String result = PredicateResultFactory.select(v -> v > 0, 1, null, "NEGATIVE");
// result == null  (predicate true, whenTrue is null — returned)
```

This is intentional: the factory makes no assumptions about your domain's null semantics.

---

### Lazy mode: branch-validated null suppliers

In `selectLazy`, only the **selected** supplier reference is validated at call time. The unselected supplier may be `null` without causing failure:

```java
// predicate true → true supplier is selected → false supplier (null) is never reached → OK
String result = PredicateResultFactory.selectLazy(v -> v > 0, 1, () -> "POSITIVE", null);
// result == "POSITIVE"

// predicate true → true supplier is null → selected supplier is null → FAIL
PredicateResultFactory.selectLazy(v -> v > 0, 1, null, () -> "NEGATIVE");
// → IllegalArgumentException: "true branch supplier must not be null when selected"
```

---

### Lazy mode: null supplier result is allowed

If the selected supplier exists but returns `null`, `null` is returned — no exception:

```java
String result = PredicateResultFactory.selectLazy(v -> v > 0, 1, () -> null, () -> "F");
// result == null
```

---

### Exactly-once supplier invocation

In `selectLazy`, the selected supplier is invoked **exactly once** per call. There is no retry, memoization, or re-evaluation:

```java
AtomicInteger calls = new AtomicInteger();
PredicateResultFactory.selectLazy(
    v -> v > 0,
    1,
    () -> { calls.incrementAndGet(); return "T"; },
    () -> "F"
);
assert calls.get() == 1;
```

---

## Exception Propagation

If the predicate throws an exception, it propagates **unchanged** from both operations:

```java
PredicateResultFactory.select(
    v -> { throw new IllegalStateException("predicate failed"); },
    input,
    "T",
    "F"
);
// → IllegalStateException: "predicate failed"
```

The factory does **not** wrap exceptions in custom types. What the predicate throws is what you receive.

---

## Type Safety

The factory enforces compile-time compatibility between:
- Predicate input type `T` and the provided input value
- Both candidates/supplier results and the declared return type `R`

```java
// Compiles cleanly
PredicateCondition<Integer> check = n -> n > 0;
String result = PredicateResultFactory.select(check, 42, "POS", "NEG");

// Compile error: Integer predicate but String input
PredicateResultFactory.select(check, "hello", "T", "F");  // ❌

// Compile error: mismatched return types
Integer result2 = PredicateResultFactory.select(check, 42, "POS", "NEG");  // ❌
```

---

## Summary

| Concept | Eager (`select`) | Lazy (`selectLazy`) |
|---------|-----------------|---------------------|
| Candidates | Pre-built values | Suppliers |
| Construction | Both always built | Only selected branch |
| Supplier invocation | N/A | Exactly once |
| Null candidates | Allowed | Supplier ref validated only on selected branch |
| Predicate null | `IllegalArgumentException` | `IllegalArgumentException` |
| Exception propagation | Unchanged | Unchanged |
| Thread safety | Safe (stateless) | Safe (stateless) |

---

**→ Continue to [Eager Selection](03-eager-selection.md)**

