# Phase 0 Research: Generic Predicate Result Factory

## Scope

Research consolidates technical decisions for implementing the specification in `request-utils` with Java 17 and repository conventions.

## Decision 1: Public API shape for eager + lazy selection

- Decision: Expose two static generic operations in a utility-style factory API (one eager with values, one lazy with suppliers).
- Rationale: Static operations minimize object lifecycle concerns, keep call sites concise, and match utility-library usage in this repository.
- Alternatives considered:
  - Instance/service bean API: rejected because behavior is stateless and does not require Spring container wiring.
  - Builder-only API: rejected because this feature needs minimal, direct true/false selection calls.

## Decision 2: Predicate type signature to support predicate-extended interfaces

- Decision: Accept predicate parameter as a generic bound compatible with extensions (for example, `P extends Predicate<? super T>` or equivalent type-safe predicate acceptance).
- Rationale: This preserves lambda and method-reference ergonomics while allowing domain-specific functional interfaces that extend predicate semantics.
- Alternatives considered:
  - Accept only `Predicate<T>` without extension-aware generic bound: workable but less expressive for custom interface preservation.
  - Raw `Predicate` type: rejected due to loss of compile-time type safety.

## Decision 3: Validation contract for null handling

- Decision: Enforce fail-fast `IllegalArgumentException` for null predicate in both operations, and in lazy mode validate only the selected supplier reference.
- Rationale: Matches clarified requirements exactly and avoids false failures from unselected branches.
- Alternatives considered:
  - `NullPointerException`: rejected because specification explicitly requires `IllegalArgumentException`.
  - Validating both suppliers upfront: rejected because it violates requirement that unselected null supplier must not fail.

## Decision 4: Lazy invocation semantics

- Decision: Evaluate predicate once and invoke only the selected supplier exactly once.
- Rationale: Ensures deterministic behavior, supports expensive supplier computations, and fulfills FR-011.
- Alternatives considered:
  - Multiple supplier invocations for caching checks: rejected because it breaks exactly-once requirement.
  - Eagerly evaluating both suppliers: rejected because it violates lazy semantics.

## Decision 5: Exception propagation behavior

- Decision: Propagate predicate-thrown exceptions unchanged.
- Rationale: Preserves caller diagnostics and aligns with explicit contract expectations.
- Alternatives considered:
  - Wrapping in custom runtime exception: rejected because it changes observable behavior and violates FR-012.

## Decision 6: Testing strategy and granularity

- Decision: Implement focused JUnit 5 unit tests covering all FR-014 cases, including supplier invocation counters for exactly-once verification.
- Rationale: Unit tests provide deterministic contract verification without environment setup and follow existing `request-utils` testing conventions.
- Alternatives considered:
  - Integration-test-only coverage: rejected due to unnecessary overhead for pure functional logic.
  - Parameterized-only mega test: rejected to keep failures isolated and easy to diagnose.

## Decision 7: Documentation placement

- Decision: Capture primary usage examples in feature quickstart and promote to module docs when implementation is complete.
- Rationale: Keeps planning artifacts executable and traceable to acceptance tests before broader documentation update.
- Alternatives considered:
  - Deferring docs to post-implementation: rejected because FR-013 requires documented examples tied to behavior.

## Clarification Resolution Status

All technical-context clarifications are resolved for planning; no open clarification items remain.
