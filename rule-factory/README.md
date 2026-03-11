# rule-factory

Generic predicate-based result factory utilities.

## Documentation

| Document | Description |
|----------|-------------|
| [Tutorial Index](docs/tutorial/00-index.md) | Start here — learning paths and overview |
| [Quick Start](docs/tutorial/01-quick-start.md) | First factory call in 5 minutes |
| [Core Concepts](docs/tutorial/02-core-concepts.md) | Architecture, both operations, validation |
| [Eager Selection](docs/tutorial/03-eager-selection.md) | Value-based selection patterns |
| [Lazy Selection](docs/tutorial/04-lazy-selection.md) | Supplier-based deferred selection |
| [Custom Predicates](docs/tutorial/05-custom-predicates.md) | Domain-specific predicate interfaces |
| [Complete Examples](docs/tutorial/06-complete-examples.md) | Real-world production examples |
| [Troubleshooting](docs/tutorial/07-troubleshooting.md) | Common issues and fixes |

## Provided APIs

- `PredicateResultFactory.select(...)` — eager true/false candidate selection
- `PredicateResultFactory.selectLazy(...)` — lazy true/false supplier selection

## Behavior Summary

- Null predicate → `IllegalArgumentException` (clear message)
- Eager mode allows null selected candidate and returns it
- Lazy mode validates only the selected supplier reference
- Lazy selected supplier returning `null` is allowed
- Lazy selected supplier is invoked exactly once
- Predicate exceptions are propagated unchanged

## Usage Examples

### Eager — lambda predicate

```java
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

String label = PredicateResultFactory.select(
    age -> age >= 18,   // predicate
    21,                 // input
    "ADULT",            // returned when true
    "MINOR"             // returned when false
);
// label == "ADULT"
```

### Eager — method-reference predicate

```java
import com.cleveloper.jufu.rulefactory.predicate.PredicateCondition;
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

PredicateCondition<String> hasText = Predicate.not(String::isBlank);

String result = PredicateResultFactory.select(
    hasText,
    "hello",
    "HAS_TEXT",
    "EMPTY"
);
// result == "HAS_TEXT"
```

### Eager — domain-specific extended predicate

```java
@FunctionalInterface
interface PremiumCheck extends Predicate<User> {}

PremiumCheck isPremium = User::isPremium;

String tier = PredicateResultFactory.select(isPremium, currentUser, "PREMIUM", "BASIC");
```

### Lazy — deferred/expensive object construction

```java
import com.cleveloper.jufu.rulefactory.predicate.PredicateResultFactory;

// Only the selected branch supplier is invoked — exactly once
UserDto dto = PredicateResultFactory.selectLazy(
    user -> user.isPremium(),
    currentUser,
    () -> buildPremiumDto(currentUser),   // invoked only if true
    () -> buildBasicDto(currentUser)      // invoked only if false
);
```

### Lazy — null unselected supplier is tolerated

```java
// False supplier is null but predicate is true → no failure
String value = PredicateResultFactory.selectLazy(
    v -> v > 0,
    1,
    () -> "POSITIVE",
    null              // unselected: tolerated
);
// value == "POSITIVE"
```

## Try It

```bash
cd /Users/abu/projects/real/java-utility-functions/rule-factory
mvn test
mvn -q -DskipTests compile
java -cp target/classes com.cleveloper.jufu.Main
```
