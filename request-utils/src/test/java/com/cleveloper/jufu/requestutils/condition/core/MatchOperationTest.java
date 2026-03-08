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
}
