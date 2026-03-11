# Troubleshooting

**Goal:** Diagnose and fix common issues with `rule-factory`.

This guide is organised by symptom. Find your problem, follow the diagnostics, apply the fix.

---

## Symptom: Wrong Value Returned

### Problem: Factory always returns the false candidate

**Symptom:**
```java
String result = PredicateResultFactory.select(user -> user.isPremium(), user, "PREMIUM", "BASIC");
// Returns "BASIC" even for premium users
```

**Diagnosis:**

1. Test the predicate in isolation first:
```java
System.out.println("isPremium: " + user.isPremium());
System.out.println("test:      " + (user.isPremium() == true));
```

2. Check that the correct `user` object is passed as the `input` argument — not captured from a different scope:
```java
// Bug: captures outer 'user' in predicate but passes different 'currentUser' as input
PredicateResultFactory.select(u -> user.isPremium(), currentUser, "T", "F");
//                                   ^^^^ wrong — should be 'u'

// Fix
PredicateResultFactory.select(u -> u.isPremium(), currentUser, "T", "F");
```

3. Verify argument order — `whenTrue` comes before `whenFalse`:
```java
// Easy swap mistake
PredicateResultFactory.select(pred, input, "BASIC", "PREMIUM");
//                                          ^^^^^^   ^^^^^^^ swapped!

// Correct
PredicateResultFactory.select(pred, input, "PREMIUM", "BASIC");
```

---

### Problem: Lazy variant always invokes the wrong supplier

**Symptom:**
```java
PredicateResultFactory.selectLazy(pred, input, trueSupplier, falseSupplier);
// trueSupplier is always called even when predicate should be false
```

**Diagnosis:**

1. Log the predicate result:
```java
boolean predicateResult = pred.test(input);
System.out.println("predicate result: " + predicateResult);
```

2. Verify supplier order — same issue as eager: true supplier is first, false supplier is second.

3. Check for captured variable issues:
```java
boolean flag = computeFlag();   // evaluated here

// Bug: predicate ignores 'input', uses captured 'flag'
PredicateResultFactory.selectLazy(
    input -> flag,   // ← always uses 'flag' regardless of 'input'
    actualInput,
    trueSupplier,
    falseSupplier
);
```

---

## Symptom: IllegalArgumentException on null predicate

### Problem: Predicate is null at call site

**Symptom:**
```java
PredicateResultFactory.select(null, input, "T", "F");
// IllegalArgumentException: predicate must not be null
```

**Diagnosis:**

Check that the predicate is initialised before use, especially for predicate constants:

```java
// Bug: null constant
public static final PredicateCondition<User> IS_PREMIUM = null;  // ← forgot to assign

// Fix
public static final PredicateCondition<User> IS_PREMIUM = User::isPremium;
```

Check method return types when building predicates dynamically:

```java
PredicateCondition<User> rule = ruleFactory.buildRule(config);
// If buildRule returns null for unknown config, rule is null here
if (rule == null) {
    throw new IllegalStateException("No rule configured for: " + config);
}
PredicateResultFactory.select(rule, user, "T", "F");
```

---

## Symptom: IllegalArgumentException on null lazy supplier

### Problem: Selected supplier reference is null

**Symptom:**
```java
PredicateResultFactory.selectLazy(pred, input, null, () -> "F");
// IllegalArgumentException: true branch supplier must not be null when selected
```

**Diagnosis:**

This happens when:
1. The predicate is true AND the true supplier is null, or
2. The predicate is false AND the false supplier is null

Remember: only the **selected** supplier is validated. The unselected supplier may be null.

```java
// This is fine — false supplier is null but predicate is true, so false is unselected
PredicateResultFactory.selectLazy(v -> v > 0, 5, () -> "POSITIVE", null);  // OK

// This fails — true supplier is null and predicate is true → selected supplier is null
PredicateResultFactory.selectLazy(v -> v > 0, 5, null, () -> "NEGATIVE");  // IllegalArgumentException
```

**Fix:** Ensure supplier for the selected branch is never null:
```java
Supplier<String> trueSupplier = someCondition ? () -> "T" : null;

// Guard before calling
if (trueSupplier == null) {
    trueSupplier = () -> "DEFAULT_TRUE";
}

PredicateResultFactory.selectLazy(pred, input, trueSupplier, () -> "F");
```

---

## Symptom: Predicate Exception Propagated

### Problem: Exception from predicate surfaces at call site

**Symptom:**
```java
PredicateResultFactory.select(
    user -> externalService.check(user),  // throws RuntimeException
    user,
    "T",
    "F"
);
// RuntimeException surfaces here
```

**Explanation:** This is correct behavior — the factory propagates predicate exceptions unchanged. There is no swallowing or wrapping.

**Fix:**

Option 1: Handle in the predicate:
```java
PredicateResultFactory.select(
    user -> {
        try {
            return externalService.check(user);
        } catch (Exception e) {
            log.warn("Check failed for user {}, defaulting to false", user.getId(), e);
            return false;  // safe fallback
        }
    },
    user,
    "T",
    "F"
);
```

Option 2: Handle at the call site:
```java
try {
    return PredicateResultFactory.select(pred, user, "T", "F");
} catch (RuntimeException e) {
    log.error("Predicate evaluation failed", e);
    return "DEFAULT";
}
```

---

## Symptom: Compilation Errors

### Problem: Type mismatch between predicate input and provided input

**Symptom:**
```java
PredicateCondition<Integer> intCheck = n -> n > 0;
PredicateResultFactory.select(intCheck, "hello", "T", "F");
// Compile error: cannot apply PredicateCondition<Integer> to String
```

**Fix:** Ensure predicate type `T` matches the input type:
```java
PredicateCondition<String> strCheck = s -> !s.isBlank();
PredicateResultFactory.select(strCheck, "hello", "T", "F");  // OK
```

---

### Problem: Candidate types incompatible with declared return type

**Symptom:**
```java
Integer result = PredicateResultFactory.select(pred, input, "TEXT", "OTHER");
// Compile error: String cannot be converted to Integer
```

**Fix:** Ensure both candidates and the assigned type are compatible:
```java
String result = PredicateResultFactory.select(pred, input, "TEXT", "OTHER");  // OK
```

---

### Problem: Lambda not accepted as custom predicate interface

**Symptom:**
```java
@FunctionalInterface
interface MyRule extends java.util.function.Predicate<User> {}

MyRule rule = user -> user.isPremium();  // OK

// But passing to standard Predicate parameter fails
someMethod(rule);  // method expects Predicate<User>, not MyRule
```

**Explanation:** `MyRule` and `Predicate<User>` are different types even though they're structurally identical. The factory's `PredicateCondition<T>` bound accepts both.

**Fix:** Use the factory which accepts any `Predicate<? super T>` extension. For your own APIs, accept `PredicateCondition<T>` or `Predicate<T>` depending on how flexible you want to be.

---

## Symptom: Unexpected Null Result

### Problem: Eager candidate is null but code expected non-null

**Diagnosis:**

The factory returns null candidates as-is — no null safety layer is added:

```java
String result = PredicateResultFactory.select(
    user -> user.isPremium(), user,
    premiumLabel,   // may be null if premiumLabel was never set
    basicLabel
);
// result may be null if the selected candidate is null
```

**Fix:** Guard before the call or after:
```java
// Option 1: guard candidates before call
String safeTrue  = premiumLabel  != null ? premiumLabel  : "PREMIUM_DEFAULT";
String safeFalse = basicLabel    != null ? basicLabel    : "BASIC_DEFAULT";
String result = PredicateResultFactory.select(pred, user, safeTrue, safeFalse);

// Option 2: guard result after call
String result = PredicateResultFactory.select(pred, user, premiumLabel, basicLabel);
if (result == null) {
    result = "FALLBACK";
}
```

---

## Symptom: Lazy Supplier Called More Than Once

**Diagnosis:**

The factory guarantees exactly-once invocation. If your supplier is being called more than once it is being called from outside the factory — check your call site for duplicate factory invocations:

```java
// Bug: factory called twice
String a = PredicateResultFactory.selectLazy(pred, input, supplier, fallback);
String b = PredicateResultFactory.selectLazy(pred, input, supplier, fallback);
// supplier was called twice because factory was called twice — expected
```

**Fix:** Cache the factory result if you need it multiple times:
```java
String result = PredicateResultFactory.selectLazy(pred, input, supplier, fallback);
// Use 'result' wherever needed — don't call the factory again
```

---

## Quick Diagnostic Checklist

When something is wrong, run through this list:

- [ ] Is the predicate non-null?
- [ ] Is the predicate testing `input` (the argument `u` / `v`) rather than a captured variable?
- [ ] Are `whenTrue` and `whenFalse` in the correct order (true first, false second)?
- [ ] For lazy: is the selected supplier non-null?
- [ ] Is the predicate result what you expect? (`System.out.println(pred.test(input))`)
- [ ] Are candidate types compatible with the declared return type?
- [ ] Is the factory being called only once per desired result?

---

## Getting More Help

- [Core Concepts](02-core-concepts.md) — Review validation and exception behavior
- [Eager Selection](03-eager-selection.md) — Review eager patterns and anti-patterns
- [Lazy Selection](04-lazy-selection.md) — Review lazy patterns and anti-patterns
- [Custom Predicates](05-custom-predicates.md) — Review predicate anti-patterns
- [Source code](../../src/main/java/com/cleveloper/jufu/rulefactory/predicate/) — Read the implementation directly

---

**→ Back to [Tutorial Index](00-index.md)**

