# Tasks: Generic Predicate Result Factory

**Feature**: `002-predicate-result-factory`  
**Branch**: `002-predicate-result-factory`  
**Date**: 2026-03-10  
**Module**: `rule-factory`  
**Input**: Design documents from `specs/002-predicate-result-factory/`  
**Prerequisites**: plan.md ✅ · spec.md ✅ · research.md ✅ · data-model.md ✅ · contracts/ ✅ · quickstart.md ✅

## Status Legend

- ✅ **DONE** — task complete and verified
- ⬜ **TODO** — task not yet started

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no blocking dependency)
- **[Story]**: Which user story this task belongs to (US1/US2/US3)
- **✅ / ⬜** replaces `[x]` / `[ ]` for already-completed vs pending tasks

---

## Phase 1: Setup (Module Infrastructure)

**Purpose**: Establish `rule-factory` module with build tooling and package skeleton.

- [x] T001 Scaffold `rule-factory` Maven module with `pom.xml`, Maven wrapper, and Spring Boot parent (rule-factory/pom.xml)
- [x] T002 Create package hierarchy `com.cleveloper.jufu.rulefactory.predicate` under `rule-factory/src/main/java/`
- [x] T003 [P] Create corresponding test package `com.cleveloper.jufu.rulefactory.predicate` under `rule-factory/src/test/java/`

**Checkpoint ✅**: Module compiles, Maven wrapper executes, test source tree exists.

---

## Phase 2: Foundational (Contract Definitions — Blocking Prerequisites)

**Purpose**: Define the public functional interface contract before any implementation or tests can be written. Blocks all user story phases.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 Define `PredicateCondition<T>` functional interface extending `Predicate<T>` in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateCondition.java`
- [x] T005 Declare `PredicateResultFactory` class shell with static method signatures for `select(...)` and `selectLazy(...)` per Contract A and Contract B in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`

**Checkpoint ✅**: `PredicateCondition` and `PredicateResultFactory` compile with correct generic signatures (`<T, R, P extends Predicate<? super T>>`); no unchecked cast warnings.

---

## Phase 3: User Story 1 — Select one of two outcomes (Priority: P1) 🎯 MVP

**Goal**: Consumer passes a predicate and two candidates (eager) or two suppliers (lazy) and receives the correct one back without writing `if/else`.

**Independent Test**: Call `select(age -> age >= 18, 21, "ADULT", "MINOR")` → `"ADULT"`. Call with `17` → `"MINOR"`. Call `selectLazy(...)` with true predicate → only true supplier reached and returned. Call with false predicate → only false supplier reached and returned.

### Tests — User Story 1

- [x] T006 [P] [US1] Implement `selectReturnsTrueCandidateWhenPredicateTrue` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T007 [P] [US1] Implement `selectReturnsFalseCandidateWhenPredicateFalse` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T008 [P] [US1] Implement `selectLazyReturnsTrueSupplierWhenPredicateTrue` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T009 [P] [US1] Implement `selectLazyReturnsFalseSupplierWhenPredicateFalse` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`

### Implementation — User Story 1

- [x] T010 [US1] Implement `select(P predicate, T input, R whenTrue, R whenFalse)` eager selection body (evaluate predicate once, return matching candidate) in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`
- [x] T011 [US1] Implement `selectLazy(P predicate, T input, Supplier<? extends R> whenTrueSupplier, Supplier<? extends R> whenFalseSupplier)` lazy selection body (evaluate predicate once, identify selected supplier, invoke exactly once) in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`

**Checkpoint ✅**: T006–T009 pass. `mvn test -Dtest=PredicateResultFactoryTest` green for US1 scenarios. User Story 1 independently testable.

---

## Phase 4: User Story 2 — Reuse domain-specific predicate interfaces (Priority: P2)

**Goal**: Consumer defines a custom functional interface that extends predicate semantics; factory accepts it without casts and behaves identically to standard `Predicate<T>` evaluation.

**Independent Test**: Define `interface AgeCheck extends Predicate<Integer> {}`; pass a lambda as `AgeCheck`; invoke `select(...)` and confirm no compile error and correct value returned. Repeat with method reference.

### Tests — User Story 2

- [x] T012 [P] [US2] Implement `supportsPredicateExtendedFunctionalInterface` covering custom-interface lambda and method-reference usage in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`

### Implementation — User Story 2

- [x] T013 [US2] Verify generic type bound `P extends Predicate<? super T>` is correctly declared in both method signatures to accept predicate-extended interfaces without unchecked casts in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`

**Checkpoint ✅**: T012 passes. Compiler accepts custom predicate interface without warnings. User Story 2 independently verifiable at compile time and at test runtime.

---

## Phase 5: User Story 3 — Predictable validation and failure behavior (Priority: P3)

**Goal**: Consumer receives `IllegalArgumentException` with a clear message for null predicate or null selected supplier; predicate-thrown exceptions propagate unchanged; all other null handling follows contract.

**Independent Test**: Call `select(null, ...)` → `IllegalArgumentException`. Call `selectLazy(pred, input, null, supplier)` when predicate is true → `IllegalArgumentException`. Call `selectLazy(pred, input, supplier, null)` when predicate is false → `IllegalArgumentException`. Call `selectLazy(pred, input, supplier, null)` when predicate is true → supplier result returned. Throw from predicate → same exception surfaces.

### Tests — User Story 3

- [x] T014 [P] [US3] Implement `nullPredicateThrowsIllegalArgumentInEager` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T015 [P] [US3] Implement `nullPredicateThrowsIllegalArgumentInLazy` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T016 [P] [US3] Implement `eagerAllowsSelectedNullCandidate` (null selected candidate returned as-is) in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T017 [P] [US3] Implement `lazyThrowsOnlyWhenReachedSupplierIsNull` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T018 [P] [US3] Implement `lazyToleratesNullUnselectedSupplier` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T019 [P] [US3] Implement `lazyAllowsSelectedSupplierReturningNull` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T020 [P] [US3] Implement `lazyInvokesOnlySelectedSupplierExactlyOnce` (use invocation counter) in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`
- [x] T021 [P] [US3] Implement `predicateExceptionIsPropagatedUnchanged` in `rule-factory/src/test/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactoryTest.java`

### Implementation — User Story 3

- [x] T022 [US3] Add null-predicate guard (`Objects.requireNonNull` or equivalent `IllegalArgumentException`) at the top of both `select` and `selectLazy` in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`
- [x] T023 [US3] Add selected-supplier null guard in `selectLazy` (validate selected supplier reference after branch decision; unselected supplier must not be validated) in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`
- [x] T024 [US3] Ensure predicate invocation is not wrapped in a try/catch that would alter exception type in `rule-factory/src/main/java/com/cleveloper/jufu/rulefactory/predicate/PredicateResultFactory.java`

**Checkpoint ✅**: T014–T021 all pass (13/13 tests green). `mvn test` in `rule-factory` exits BUILD SUCCESS. All FR-001 through FR-014 requirements covered.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, repository-wide consistency, and final validation steps.

### Documentation

- [x] T025 [P] Write `rule-factory/README.md` with module overview, behavior summary, and `mvn test` quick-start commands
- [x] T026 [P] Expand `rule-factory/README.md` with full usage examples section: eager lambda example, eager method-reference example, lazy expensive-supplier example (FR-013 / SC-004) in `rule-factory/README.md`
- [x] T027 [P] Update `CLAUDE.md` — add `rule-factory` module entry to Project Overview, Project Structure diagram, and Build/Test Commands so AI agents and contributors are aware of the module in `CLAUDE.md`
- [x] T028 Update `specs/002-predicate-result-factory/spec.md` status from `Draft` to `Complete` in `specs/002-predicate-result-factory/spec.md`
- [x] T029 [P] Confirm `specs/002-predicate-result-factory/quickstart.md` step 5 (documentation alignment) is satisfied: verify README examples are in sync with final implementation in `rule-factory/README.md`

### Validation

- [x] T030 Run full `rule-factory` test suite from clean state and confirm all 13 tests pass: `cd rule-factory && ./mvnw clean test`
- [x] T031 [P] Verify no compiler warnings for unchecked casts in `rule-factory` production sources: `cd rule-factory && ./mvnw clean compile -Xlint:unchecked 2>&1 | grep -i unchecked`
- [x] T032 [P] Mark `tasks.md` itself as the final artifact: confirm `specs/002-predicate-result-factory/tasks.md` is committed in `specs/002-predicate-result-factory/tasks.md`

**Checkpoint**: All polish tasks complete → feature branch ready for review/merge.

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)          → no dependencies; start immediately
Phase 2 (Foundational)   → depends on Phase 1; BLOCKS Phases 3–5
Phase 3 (US1 — P1)       → depends on Phase 2; independent of US2/US3
Phase 4 (US2 — P2)       → depends on Phase 2; independent of US1/US3
Phase 5 (US3 — P3)       → depends on Phase 2; independent of US1/US2
Phase 6 (Polish)         → depends on Phases 3+4+5 all complete
```

### User Story Dependencies

| Story | Depends On | Blocks |
|-------|------------|--------|
| US1 (P1) | Phase 2 (T004–T005) | Nothing — MVP increment |
| US2 (P2) | Phase 2 (T004–T005) | Nothing — independently testable |
| US3 (P3) | Phase 2 (T004–T005) | Nothing — independently testable |

> All three user stories share only the foundational type signatures (T004–T005). They can proceed in any order after Phase 2.

### Within Each User Story

- Tests (T006–T009, T012, T014–T021) declared before or alongside implementation
- Type contract (T013) is a compile-time verification, not a runtime implementation task
- Polish tasks (T026–T032) must come after all stories are green

---

## Parallel Execution Examples

### User Story 1 — Parallel test writing

```
# All four US1 test methods can be written simultaneously:
T006  selectReturnsTrueCandidateWhenPredicateTrue
T007  selectReturnsFalseCandidateWhenPredicateFalse
T008  selectLazyReturnsTrueSupplierWhenPredicateTrue
T009  selectLazyReturnsFalseSupplierWhenPredicateFalse
```

### User Story 3 — Parallel test writing

```
# All eight US3 test methods can be written simultaneously:
T014  nullPredicateThrowsIllegalArgumentInEager
T015  nullPredicateThrowsIllegalArgumentInLazy
T016  eagerAllowsSelectedNullCandidate
T017  lazyThrowsOnlyWhenReachedSupplierIsNull
T018  lazyToleratesNullUnselectedSupplier
T019  lazyAllowsSelectedSupplierReturningNull
T020  lazyInvokesOnlySelectedSupplierExactlyOnce
T021  predicateExceptionIsPropagatedUnchanged
```

### Polish Phase — Parallel doc tasks

```
# These touch different files and can run in parallel:
T026  rule-factory/README.md  (usage examples)
T027  CLAUDE.md               (module registration)
T028  spec.md                 (status update)
T029  quickstart.md check     (documentation alignment)
T031  compile lint check      (verification only)
```

---

## Implementation Strategy

### MVP Scope (User Story 1 Only)

1. Complete Phase 1 (Setup) — ✅ Done
2. Complete Phase 2 (Foundational) — ✅ Done
3. Complete Phase 3 (US1) — ✅ Done
4. **Validate**: `mvn test -Dtest=PredicateResultFactoryTest` — ✅ 13/13 pass

> MVP was achieved at T011. The core value (select one of two outcomes) is fully working.

### Incremental Delivery (Actual)

| Increment | Tasks | Status |
|-----------|-------|--------|
| Setup | T001–T003 | ✅ Complete |
| Foundational contracts | T004–T005 | ✅ Complete |
| US1 — Core selection | T006–T011 | ✅ Complete |
| US2 — Predicate extension | T012–T013 | ✅ Complete |
| US3 — Validation & failure | T014–T024 | ✅ Complete |
| Polish — README baseline | T025 | ✅ Complete |
| Polish — Full docs & CLAUDE.md | T026–T032 | ⬜ Remaining |

### Remaining Work (Phase 6 incomplete)

```
T026  Expand README with full usage examples
T027  Update CLAUDE.md — add rule-factory module
T028  Update spec.md status → Complete
T029  Confirm quickstart.md doc alignment
T030  Clean test run verification
T031  Unchecked-cast lint check
T032  Confirm tasks.md committed
```

---

## Contract Reference

| FR | Requirement | Covered By | Status |
|----|-------------|------------|--------|
| FR-001 | Two generic selection operations (eager + lazy) | T010, T011 | ✅ |
| FR-002 | Eager: return true candidate when predicate true | T010, T006 | ✅ |
| FR-003 | Eager: return false candidate when predicate false | T010, T007 | ✅ |
| FR-004 | Lazy: evaluate and return only selected branch | T011, T008, T009 | ✅ |
| FR-005 | Support custom predicate-extended interfaces | T004, T013, T012 | ✅ |
| FR-006 | Compile-time type compatibility enforced | T005, T013 | ✅ |
| FR-007 | Null predicate → `IllegalArgumentException` | T022, T014, T015 | ✅ |
| FR-008 | Eager null candidates allowed, returned as-is | T010, T016 | ✅ |
| FR-009 | Lazy: only selected null supplier fails | T023, T017, T018 | ✅ |
| FR-010 | Lazy: selected supplier returning null → return null | T011, T019 | ✅ |
| FR-011 | Lazy: selected supplier invoked exactly once | T011, T020 | ✅ |
| FR-012 | Predicate exception propagated unchanged | T024, T021 | ✅ |
| FR-013 | Usage documentation with examples | T025 (partial), T026 (TODO) | ⬜ |
| FR-014 | Tests for all 10 contract matrix scenarios | T006–T009, T012, T014–T021 | ✅ |
| FR-015 | Naming, package, doc style aligned to repo | T002, T003, T025 | ✅ |
| FR-016 | Java 17 compatible | T001 (pom.xml), T005 | ✅ |

---

## Notes

- `[P]` tasks operate on different files or are read-only verifications — safe to parallelize
- `[US1/US2/US3]` labels map directly to spec.md user stories for traceability
- Completed tasks use `[x]` (checked checkbox) per standard markdown; pending use `[ ]`
- All 13 tests pass as of 2026-03-10 (`mvn test` in `rule-factory` → BUILD SUCCESS)
- The only open work is Phase 6 polish: documentation expansion, CLAUDE.md update, and final validation sweep
- FR-013 is the sole incomplete functional requirement — addressed by T026

