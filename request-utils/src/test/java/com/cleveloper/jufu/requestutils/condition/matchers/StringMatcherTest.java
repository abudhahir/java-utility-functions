package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.MatchOperation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StringMatcherTest {

    @Test
    void shouldMatchEquals() {
        assertTrue(StringMatcher.matches("test", "test", MatchOperation.EQUALS, false));
        assertFalse(StringMatcher.matches("test", "other", MatchOperation.EQUALS, false));
    }

    @Test
    void shouldMatchEqualsCaseInsensitive() {
        assertTrue(StringMatcher.matches("TEST", "test", MatchOperation.EQUALS, true));
        assertTrue(StringMatcher.matches("test", "TEST", MatchOperation.EQUALS, true));
    }

    @Test
    void shouldMatchContains() {
        assertTrue(StringMatcher.matches("hello world", "world", MatchOperation.CONTAINS, false));
        assertFalse(StringMatcher.matches("hello world", "foo", MatchOperation.CONTAINS, false));
    }

    @Test
    void shouldMatchStartsWith() {
        assertTrue(StringMatcher.matches("hello world", "hello", MatchOperation.STARTS_WITH, false));
        assertFalse(StringMatcher.matches("hello world", "world", MatchOperation.STARTS_WITH, false));
    }

    @Test
    void shouldMatchEndsWith() {
        assertTrue(StringMatcher.matches("hello world", "world", MatchOperation.ENDS_WITH, false));
        assertFalse(StringMatcher.matches("hello world", "hello", MatchOperation.ENDS_WITH, false));
    }

    @Test
    void shouldMatchRegex() {
        assertTrue(StringMatcher.matches("test123", "test\\d+", MatchOperation.REGEX, false));
        assertFalse(StringMatcher.matches("testabc", "test\\d+", MatchOperation.REGEX, false));
    }

    @Test
    void shouldHandleNullActualValue() {
        assertFalse(StringMatcher.matches(null, "test", MatchOperation.EQUALS, false));
    }
}
