# Data Model: Generic Predicate Result Factory

## Overview

This feature is behavior-centric and introduces a logical data model for API contracts rather than persistent storage entities.

## Entity: PredicateCondition<T>

- Purpose: Functional contract that evaluates input `T` to a boolean decision.
- Fields:
  - `input: T` (evaluation subject)
- Validation rules:
  - Predicate instance must be non-null before evaluation.
  - Input value may be null; predicate decides whether null input is supported.
- Relationships:
  - Used by `SelectionInput<T, R>` to determine branch selection.

## Entity: SelectionInput<T, R>

- Purpose: Full call input for eager or lazy selection operations.
- Fields (eager variant):
  - `predicate: PredicateCondition<T>`
  - `input: T`
  - `whenTrue: R`
  - `whenFalse: R`
- Fields (lazy variant):
  - `predicate: PredicateCondition<T>`
  - `input: T`
  - `whenTrueSupplier: Supplier<R>`
  - `whenFalseSupplier: Supplier<R>`
- Validation rules:
  - `predicate` is required; null is invalid (`IllegalArgumentException`).
  - Eager candidates may be null and are returned as selected.
  - Lazy suppliers are branch-validated: selected supplier reference must be non-null; unselected supplier may be null.

## Entity: SelectionOutcome<R>

- Purpose: Result selected from true/false branch.
- Fields:
  - `selectedValue: R`
  - `selectedBranch: TRUE | FALSE` (conceptual metadata for tests/docs)
- Validation rules:
  - `selectedValue` may be null if selected eager value is null or selected supplier returns null.
- Relationships:
  - Derived from evaluating `PredicateCondition<T>` against `SelectionInput<T, R>`.

## State Transitions

## Transition A: Eager Selection

1. Validate `predicate` is non-null.
2. Evaluate predicate once with input.
3. If true, return `whenTrue`; else return `whenFalse`.
4. If predicate throws, propagate unchanged.

## Transition B: Lazy Selection

1. Validate `predicate` is non-null.
2. Evaluate predicate once with input.
3. Identify selected supplier based on branch.
4. Validate selected supplier reference is non-null.
5. Invoke selected supplier exactly once and return produced value.
6. If predicate throws, propagate unchanged.

## Type Constraints

- Predicate input type must be compile-time compatible with provided input value.
- Branch values/suppliers must be compile-time compatible with declared return type `R`.
- API must remain lambda- and method-reference-friendly for predicate-extended interfaces.

