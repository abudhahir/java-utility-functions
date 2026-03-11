package com.cleveloper.jufu.rulefactory.predicate;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PredicateResultFactoryTest {

    @Test
    void selectReturnsTrueCandidateWhenPredicateTrue() {
        String result = PredicateResultFactory.select(v -> v > 10, 11, "T", "F");
        assertEquals("T", result);
    }

    @Test
    void selectReturnsFalseCandidateWhenPredicateFalse() {
        String result = PredicateResultFactory.select(v -> v > 10, 1, "T", "F");
        assertEquals("F", result);
    }

    @Test
    void selectLazyReturnsTrueSupplierWhenPredicateTrue() {
        String result = PredicateResultFactory.selectLazy(v -> v > 10, 11, () -> "T", () -> "F");
        assertEquals("T", result);
    }

    @Test
    void selectLazyReturnsFalseSupplierWhenPredicateFalse() {
        String result = PredicateResultFactory.selectLazy(v -> v > 10, 1, () -> "T", () -> "F");
        assertEquals("F", result);
    }

    @Test
    void nullPredicateThrowsIllegalArgumentInEager() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PredicateResultFactory.select(null, 1, "T", "F")
        );
        assertEquals("predicate must not be null", ex.getMessage());
    }

    @Test
    void nullPredicateThrowsIllegalArgumentInLazy() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PredicateResultFactory.selectLazy(null, 1, () -> "T", () -> "F")
        );
        assertEquals("predicate must not be null", ex.getMessage());
    }

    @Test
    void eagerAllowsSelectedNullCandidate() {
        String result = PredicateResultFactory.select(v -> v > 0, 1, null, "F");
        assertNull(result);
    }

    @Test
    void lazyThrowsOnlyWhenReachedSupplierIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> PredicateResultFactory.selectLazy(v -> v > 0, 1, null, () -> "F")
        );
        assertEquals("true branch supplier must not be null when selected", ex.getMessage());
    }

    @Test
    void lazyToleratesNullUnselectedSupplier() {
        String result = PredicateResultFactory.selectLazy(v -> v > 0, 1, () -> "T", null);
        assertEquals("T", result);
    }

    @Test
    void lazyAllowsSelectedSupplierReturningNull() {
        String result = PredicateResultFactory.selectLazy(v -> v > 0, 1, () -> null, () -> "F");
        assertNull(result);
    }

    @Test
    void lazyInvokesOnlySelectedSupplierExactlyOnce() {
        AtomicInteger trueCalls = new AtomicInteger();
        AtomicInteger falseCalls = new AtomicInteger();

        String result = PredicateResultFactory.selectLazy(
                v -> v > 0,
                1,
                () -> {
                    trueCalls.incrementAndGet();
                    return "T";
                },
                () -> {
                    falseCalls.incrementAndGet();
                    return "F";
                }
        );

        assertEquals("T", result);
        assertEquals(1, trueCalls.get());
        assertEquals(0, falseCalls.get());
    }

    @Test
    void supportsPredicateExtendedFunctionalInterface() {
        PredicateCondition<Integer> cond = value -> value % 2 == 0;
        String result = PredicateResultFactory.select(cond, 2, "EVEN", "ODD");
        assertEquals("EVEN", result);
    }

    @Test
    void predicateExceptionIsPropagatedUnchanged() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> PredicateResultFactory.select(v -> {
                    throw new IllegalStateException("boom");
                }, 1, "T", "F")
        );
        assertEquals("boom", ex.getMessage());
    }
}

