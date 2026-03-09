package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatchOperationTest {

    @Test
    void shouldHaveEqualsOperation() {
        assertNotNull(MatchOperation.EQUALS);
    }

    @Test
    void shouldHaveContainsOperation() {
        assertNotNull(MatchOperation.CONTAINS);
    }

    @Test
    void shouldHaveStartsWithOperation() {
        assertNotNull(MatchOperation.STARTS_WITH);
    }

    @Test
    void shouldHaveEndsWithOperation() {
        assertNotNull(MatchOperation.ENDS_WITH);
    }

    @Test
    void shouldHaveRegexOperation() {
        assertNotNull(MatchOperation.REGEX);
    }

    @Test
    void shouldHaveExactlyFiveValues() {
        assertEquals(5, MatchOperation.values().length);
    }

    @Test
    void shouldConvertFromString() {
        assertEquals(MatchOperation.EQUALS, MatchOperation.valueOf("EQUALS"));
        assertEquals(MatchOperation.CONTAINS, MatchOperation.valueOf("CONTAINS"));
        assertEquals(MatchOperation.STARTS_WITH, MatchOperation.valueOf("STARTS_WITH"));
        assertEquals(MatchOperation.ENDS_WITH, MatchOperation.valueOf("ENDS_WITH"));
        assertEquals(MatchOperation.REGEX, MatchOperation.valueOf("REGEX"));
    }

    @Test
    void shouldContainAllExpectedValues() {
        MatchOperation[] values = MatchOperation.values();
        assertTrue(java.util.Arrays.asList(values).contains(MatchOperation.EQUALS));
        assertTrue(java.util.Arrays.asList(values).contains(MatchOperation.CONTAINS));
        assertTrue(java.util.Arrays.asList(values).contains(MatchOperation.STARTS_WITH));
        assertTrue(java.util.Arrays.asList(values).contains(MatchOperation.ENDS_WITH));
        assertTrue(java.util.Arrays.asList(values).contains(MatchOperation.REGEX));
    }

    @Test
    void shouldThrowExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () ->
                MatchOperation.valueOf("INVALID"));
    }
}
