# Feature Specification: Generic Predicate Result Factory

**Feature Branch**: `002-predicate-result-factory`  
**Created**: 2026-03-10  
**Status**: Complete  
**Input**: User description: "a generic factory that takes a predicate-extended functional interface and two objects to be returned one each for true and false"

## Problem Statement

Consumers currently repeat conditional selection logic across modules when they need to choose one of two values based on a predicate-like rule. This leads to duplicated branching code, inconsistent validation behavior, and unclear exception handling semantics.

## Goals

- Provide reusable factory contracts for true/false object selection in both eager and lazy forms.
- Support custom functional interfaces that extend predicate semantics while preserving lambda usability.
- Define consistent behavior for invalid inputs and predicate failures.
- Align with Java 17 usage expectations and repository documentation/testing conventions.

## Non-Goals

- Supporting more than two candidate outcomes.
- Introducing asynchronous execution behavior.
- Adding policy engines, rule chaining, or weighted decisioning.
- Defining module-internal implementation classes in this specification.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Select one of two outcomes (Priority: P1)

As a library consumer, I want to pass a predicate-like condition and two candidate objects (or two candidate suppliers) so I can receive the correct candidate without writing repeated `if/else` logic.

**Why this priority**: This is the core user value and minimum viable behavior.

**Independent Test**: Invoke the factory once with a condition that evaluates to true and once with a condition that evaluates to false, then verify selected outputs.

**Acceptance Scenarios**:

1. **Given** two candidate objects and a condition that evaluates to true, **When** the eager factory is called, **Then** the true candidate object is returned.
2. **Given** two candidate objects and a condition that evaluates to false, **When** the eager factory is called, **Then** the false candidate object is returned.
3. **Given** two candidate suppliers and a condition that evaluates to true, **When** the lazy factory is called, **Then** only the true supplier is reached and its value is returned.
4. **Given** two candidate suppliers and a condition that evaluates to false, **When** the lazy factory is called, **Then** only the false supplier is reached and its value is returned.

---

### User Story 2 - Reuse domain-specific predicate interfaces (Priority: P2)

As a library consumer, I want to use a custom functional interface that extends predicate behavior so I can keep domain naming while still using the generic factory.

**Why this priority**: Domain-specific predicate contracts improve readability and adoption across modules.

**Independent Test**: Define a custom predicate-extended interface, invoke the factory with it, and confirm behavior matches standard predicate evaluation.

**Acceptance Scenarios**:

1. **Given** a custom functional interface that extends predicate behavior, **When** it is passed to the factory, **Then** the factory accepts it without type casts.
2. **Given** the custom interface implemented via lambda or method reference, **When** the factory is called, **Then** selection behavior is identical to standard predicate evaluation.

---

### User Story 3 - Receive predictable validation and failure behavior (Priority: P3)

As a library consumer, I want explicit validation and failure semantics so I can safely integrate the factory and diagnose failures quickly.

**Why this priority**: Predictable failure behavior reduces hidden defects and lowers integration risk.

**Independent Test**: Exercise null predicate input, null candidates, and predicate-thrown exceptions and verify contract-defined outcomes.

**Acceptance Scenarios**:

1. **Given** a null predicate argument, **When** either factory variant is called, **Then** it fails fast with `IllegalArgumentException` and a clear message identifying the predicate argument.
2. **Given** null eager candidates and a valid predicate, **When** the selected branch resolves to null, **Then** null is returned without additional failure.
3. **Given** a lazy variant call where only the reached supplier is null, **When** that branch is selected, **Then** `IllegalArgumentException` is thrown with a clear message.
4. **Given** a lazy variant where the selected supplier exists but returns null, **When** selected, **Then** null is returned.
5. **Given** a lazy variant call, **When** a branch is selected, **Then** the selected supplier is invoked exactly once.
6. **Given** a predicate that throws an exception, **When** either factory variant is called, **Then** the same exception is propagated.

### Edge Cases

- Predicate argument is null.
- In eager mode, one candidate is null while the non-null candidate is not selected.
- In eager mode, both candidates are null.
- In lazy mode, unselected supplier is null (must not fail).
- In lazy mode, selected supplier is null (must fail with `IllegalArgumentException`).
- In lazy mode, selected supplier returns null (allowed; returns null).
- Predicate input value is null and the predicate either supports or rejects null input.
- Predicate evaluation throws checked-to-runtime wrapped or runtime exceptions.
- True and false candidates reference the same object instance.

## Requirements *(mandatory)*

### API Proposal

The feature will expose two generic factory operations with the following contracts:

- Operation A (eager):
  - Inputs:
    - One predicate-extended functional interface instance.
    - One condition input value.
    - One candidate object for the true branch.
    - One candidate object for the false branch.
  - Output:
    - Exactly one selected candidate object from true/false inputs.

- Operation B (lazy):
  - Inputs:
    - One predicate-extended functional interface instance.
    - One condition input value.
    - One supplier for the true branch value.
    - One supplier for the false branch value.
  - Output:
    - Exactly one selected supplier result from true/false supplier inputs.

- Shared contract expectations:
  - Predicate extension types are accepted as long as they conform to predicate semantics.
  - Return type is consistent across both branch candidates and enforced at compile time.
  - Java 17-compatible generics and functional interface usage are required.

### Type-Safety Considerations

- The factory contract must enforce compile-time compatibility between:
  - Predicate input type and evaluated input value.
  - True/false candidate types and declared return type.
- The API must avoid requiring unchecked casts in normal usage.
- Consumers must be able to use subtype candidates where compatible with the declared return type.
- The contract must remain lambda-friendly and method-reference-friendly for predicate-extended interfaces.

### Validation Behavior

- Null predicate (eager or lazy): fail fast with `IllegalArgumentException` and a clear message identifying the predicate argument.
- Eager mode null candidates: allowed; whichever branch is selected is returned even if null.
- Lazy mode supplier null handling: only the reached branch is validated; if reached supplier reference is null, throw `IllegalArgumentException` with a clear message. Unreached supplier null is tolerated.
- Lazy mode supplier result null handling: if reached supplier returns null, return null.
- Lazy mode invocation rule: invoke the reached supplier exactly once per factory call.
- Exceptions from predicate: propagate unchanged to caller; no default wrapping or suppression.

### Functional Requirements

- **FR-001**: System MUST provide two generic selection operations (eager object-based and lazy supplier-based) that accept a predicate-extended functional interface and a condition input value.
- **FR-002**: In eager mode, system MUST return the true candidate when the predicate evaluates to true.
- **FR-003**: In eager mode, system MUST return the false candidate when the predicate evaluates to false.
- **FR-004**: In lazy mode, system MUST evaluate and return only the selected branch supplier result.
- **FR-005**: System MUST support custom functional interfaces that extend predicate semantics.
- **FR-006**: System MUST enforce compile-time type compatibility for predicate input and branch return types.
- **FR-007**: System MUST fail fast with `IllegalArgumentException` when the predicate argument is null, with an explicit message.
- **FR-008**: In eager mode, system MUST allow null true/false candidates and return the selected candidate as-is.
- **FR-009**: In lazy mode, system MUST throw `IllegalArgumentException` only when the selected supplier reference is null, and MUST NOT fail due to an unselected null supplier.
- **FR-010**: In lazy mode, system MUST return null when the selected non-null supplier returns null.
- **FR-011**: In lazy mode, system MUST invoke the selected supplier exactly once per call.
- **FR-012**: System MUST propagate predicate-thrown exceptions unchanged.
- **FR-013**: System MUST provide usage documentation with examples for lambda and method-reference use across eager and lazy variants.
- **FR-014**: System MUST include tests for true branch, false branch, null predicate, null eager candidates, selected-null lazy supplier reference, unselected-null lazy supplier reference, selected-supplier-returns-null, exactly-once supplier invocation, custom predicate extension, and exception propagation.
- **FR-015**: System MUST align naming, package placement, and documentation style with repository conventions.
- **FR-016**: System MUST be compatible with Java 17 language/runtime expectations used by this repository.

### Usage Examples

- Eager: Select a status label based on an age-check predicate and two string candidates.
- Eager: Select one of two strategy objects based on a request classification predicate.
- Lazy: Select one of two expensive object constructions via true/false suppliers.

### Test Scenarios

- Happy path: eager predicate returns true.
- Happy path: eager predicate returns false.
- Happy path: lazy predicate returns true and only true supplier is reached.
- Happy path: lazy predicate returns false and only false supplier is reached.
- Predicate extension interface accepted without casting.
- Null predicate triggers `IllegalArgumentException` with explicit message.
- Eager null true candidate is selected and returned.
- Eager null false candidate is selected and returned.
- Eager both candidates null return null.
- Lazy selected supplier reference null triggers `IllegalArgumentException` with explicit message.
- Lazy unselected supplier reference null does not fail call.
- Lazy selected supplier returns null and call returns null.
- Lazy selected supplier is invoked exactly once.
- Predicate throws exception and exception is propagated.
- Type compatibility checks compile for valid generic usage and reject invalid pairings.

### Key Entities *(include if feature involves data)*

- **PredicateCondition<T>**: Predicate-extended functional contract used to evaluate the input value.
- **SelectionInput<T, R>**: Conceptual input set containing condition input plus true/false candidates.
- **SelectionOutcome<R>**: The returned candidate determined by predicate evaluation.

### Assumptions

- This feature is intended for shared utility use across repository modules rather than one module-specific use case.
- Validation exception type follows existing repository practices for argument validation.
- Documentation and tests follow existing repository layout and naming conventions.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In acceptance tests, 100% of true/false selection scenarios return the expected candidate.
- **SC-002**: In validation tests, 100% of null predicate calls fail with the documented validation behavior.
- **SC-003**: In error-propagation tests, 100% of predicate-thrown exceptions are surfaced unchanged to callers.
- **SC-004**: At least 3 documented usage examples are understandable to first-time consumers and map directly to executable tests.
- **SC-005**: At least 90% of pilot consumers can integrate the factory in under 10 minutes without custom branching code.

## Acceptance Criteria

- Feature behavior matches all acceptance scenarios in User Stories 1-3.
- Functional requirements FR-001 through FR-016 are validated by tests and documentation review.
- Edge-case scenarios are covered in automated tests.
- Specification contains no unresolved clarification markers and is ready for planning.
