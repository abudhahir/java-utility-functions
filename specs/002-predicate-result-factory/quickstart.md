# Quickstart: Generic Predicate Result Factory

## Goal

Implement and verify eager and lazy predicate-based selection in `rule-factory` with Java 17 semantics.

## 1) Implement API surface

Create factory API in:

- `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`
- `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateCondition.java`

Provide two operations:

- `select(...)` for eager true/false candidates
- `selectLazy(...)` for lazy true/false suppliers

## 2) Add unit tests

Create tests in:

- `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`

Cover all required scenarios from FR-014:

- eager true branch, eager false branch
- lazy true branch, lazy false branch
- null predicate -> `IllegalArgumentException`
- eager null candidates (selected null allowed)
- lazy selected supplier null -> `IllegalArgumentException`
- lazy unselected supplier null tolerated
- lazy selected supplier returns null
- selected supplier invoked exactly once
- custom predicate extension support
- predicate exception propagation

## 3) Example usage patterns

### Eager with lambda

```java
String status = PredicateResultFactory.select(age -> age >= 18, 21, "ADULT", "MINOR");
```

### Eager with method reference

```java
Predicate<String> hasText = String::isBlank;
String label = PredicateResultFactory.select(hasText.negate(), "hello", "TEXT", "EMPTY");
```

### Lazy with expensive suppliers

```java
UserDto dto = PredicateResultFactory.selectLazy(
    user -> user.isPremium(),
    currentUser,
    () -> buildPremiumDto(currentUser),
    () -> buildBasicDto(currentUser)
);
```

## 4) Verify locally

```bash
cd /Users/abu/projects/real/java-utility-functions/rule-factory
mvn test -Dtest=PredicateResultFactoryTest
mvn test
```

## 5) Documentation alignment

After tests pass, sync final examples into module docs as needed:

- `rule-factory/README.md`
