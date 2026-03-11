# Contract: Predicate Result Factory Public API

## Interface Scope

This contract defines the public Java API exposed by `request-utils` for two-outcome selection based on predicate semantics.

## Contract A: Eager Selection

```java
public static <T, R, P extends java.util.function.Predicate<? super T>> R select(
    P predicate,
    T input,
    R whenTrue,
    R whenFalse
)
```

### Behavioral Requirements

- Returns `whenTrue` when predicate evaluates to `true`.
- Returns `whenFalse` when predicate evaluates to `false`.
- If `predicate` is null, throws `IllegalArgumentException` with a message naming `predicate`.
- `whenTrue` and `whenFalse` may be null; selected value is returned as-is.
- Exceptions thrown by predicate are propagated unchanged.

## Contract B: Lazy Selection

```java
public static <T, R, P extends java.util.function.Predicate<? super T>> R selectLazy(
    P predicate,
    T input,
    java.util.function.Supplier<? extends R> whenTrueSupplier,
    java.util.function.Supplier<? extends R> whenFalseSupplier
)
```

### Behavioral Requirements

- Evaluates predicate once.
- Selects exactly one supplier based on predicate result.
- Validates only selected supplier reference:
  - selected supplier null -> `IllegalArgumentException` with message naming selected supplier argument.
  - unselected supplier null -> tolerated and must not fail call.
- Invokes selected supplier exactly once.
- Returns selected supplier result (including null).
- If `predicate` is null, throws `IllegalArgumentException` with a message naming `predicate`.
- Exceptions thrown by predicate are propagated unchanged.

## Type-Safety Contract

- API enforces compile-time compatibility across:
  - predicate input type and provided input value
  - true/false candidates and declared return type
- API supports predicate-extended functional interfaces and lambda/method-reference usage without unchecked casts.

## Testable Contract Matrix

- True/false eager selection
- True/false lazy selection
- Null predicate validation
- Eager null candidate behavior
- Lazy selected null supplier reference behavior
- Lazy unselected null supplier tolerance
- Lazy selected supplier returns null behavior
- Lazy selected supplier exactly-once invocation
- Predicate exception propagation
- Predicate extension acceptance

