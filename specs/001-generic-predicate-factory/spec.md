# Feature Specification: Generic Predicate-Based Factory

**Feature Branch**: `001-generic-predicate-factory`  
**Created**: 2026-03-10  
**Status**: Draft  
**Input**: User description: "create a generic factory which takes a predicate extended functional interface and two objects to be returned one each for true and false"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Select one of two values via predicate (Priority: P1)

As a developer, I want a generic factory method that accepts a predicate-like functional interface and two candidate return values, so I can deterministically choose one value based on a condition.

**Why this priority**: This is the core behavior and minimum viable feature.

**Independent Test**: Can be fully tested by invoking the factory with a predicate that returns true and false and verifying the selected object.

**Acceptance Scenarios**:

1. **Given** a predicate that evaluates to true and candidates `A` and `B`, **When** the factory is invoked, **Then** it returns `A`.
2. **Given** a predicate that evaluates to false and candidates `A` and `B`, **When** the factory is invoked, **Then** it returns `B`.

---

### User Story 2 - Use a custom extended predicate interface (Priority: P2)

As a developer, I want to pass an extended functional interface (beyond plain `Predicate<T>`) so I can attach richer semantics while keeping lambda compatibility.

**Why this priority**: This enables library extensibility and domain-friendly APIs.

**Independent Test**: Can be tested by defining a custom `@FunctionalInterface` that extends `Predicate<T>` and using it in the factory.

**Acceptance Scenarios**:

1. **Given** a custom interface extending `Predicate<T>`, **When** it is passed to the factory, **Then** the factory accepts it and evaluates correctly.
2. **Given** a lambda assigned to the custom interface, **When** factory is invoked, **Then** behavior matches a standard predicate invocation.

---

### User Story 3 - Handle invalid and exceptional input consistently (Priority: P3)

As a developer, I want clear validation and exception semantics, so I can use the factory safely and debug failures quickly.

**Why this priority**: Predictable failure behavior prevents silent bugs in consuming modules.

**Independent Test**: Can be tested by passing null predicate/candidates and by using a predicate that throws.

**Acceptance Scenarios**:

1. **Given** a null predicate, **When** the factory is invoked, **Then** it throws `NullPointerException` with a clear message.
2. **Given** a predicate that throws runtime exception, **When** invoked, **Then** the exception is propagated unchanged.

---

### Edge Cases

- Predicate is null.
- Both true/false candidates are null.
- One candidate is null while selected branch resolves to null.
- Predicate throws runtime exception.
- Candidate types use inheritance (e.g., true object is subtype of return type).
- Condition input object is null and predicate supports/does not support null.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a generic factory API that accepts an extended predicate functional interface, an input value, and two candidate return objects.
- **FR-002**: System MUST return the "true candidate" when predicate evaluates to true.
- **FR-003**: System MUST return the "false candidate" when predicate evaluates to false.
- **FR-004**: System MUST be type-safe at compile time for input and return generics.
- **FR-005**: System MUST accept interfaces that extend `java.util.function.Predicate<T>`.
- **FR-006**: System MUST validate that predicate argument is non-null and fail fast with explicit exception.
- **FR-007**: System MUST allow null candidate values unless project policy later forbids it.
- **FR-008**: System MUST propagate predicate-thrown exceptions without wrapping by default.
- **FR-009**: System MUST include JavaDoc and usage examples for lambda and method reference usage.
- **FR-010**: System MUST include unit tests for true/false selection, null predicate, null candidates, custom extended predicate type, and exception propagation.

### Key Entities *(include if feature involves data)*

- **ExtendedPredicate<T>**: A custom `@FunctionalInterface` extending `Predicate<T>` and used as the condition contract.
- **FactorySelectionInput<T, R>**: Conceptual tuple of condition input (`T`) and candidate outputs (`R trueCandidate`, `R falseCandidate`).
- **FactoryDecisionResult<R>**: The selected return value based on predicate evaluation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can complete true/false selection integration with at most one method call and no explicit `if/else` in consumer code.
- **SC-002**: Unit tests cover 100% of branch paths (true branch, false branch, validation failure, predicate exception).
- **SC-003**: API compiles without unchecked warnings in Java 17 for normal generic use.
- **SC-004**: At least two documentation examples (lambda and method reference) compile and execute successfully in tests.

## Proposed API (Design Notes)

```java
@FunctionalInterface
public interface ExtendedPredicate<T> extends java.util.function.Predicate<T> {
}

public final class ConditionalFactory {
    private ConditionalFactory() {}

    public static <T, R> R select(
            ExtendedPredicate<? super T> predicate,
            T input,
            R onTrue,
            R onFalse
    ) {
        java.util.Objects.requireNonNull(predicate, "predicate must not be null");
        return predicate.test(input) ? onTrue : onFalse;
    }
}
```

## Non-Goals

- Multi-branch selection (ternary+ cases).
- Lazy suppliers for deferred branch object creation.
- Async/reactive predicate evaluation.
- Policy-based exception wrapping in this initial version.

