package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroupOperatorTest {

    @Test
    void shouldHaveAndOperator() {
        assertNotNull(GroupOperator.AND);
    }

    @Test
    void shouldHaveOrOperator() {
        assertNotNull(GroupOperator.OR);
    }

    @Test
    void shouldHaveExactlyTwoValues() {
        assertEquals(2, GroupOperator.values().length);
    }

    @Test
    void shouldConvertFromString() {
        assertEquals(GroupOperator.AND, GroupOperator.valueOf("AND"));
        assertEquals(GroupOperator.OR, GroupOperator.valueOf("OR"));
    }

    @Test
    void shouldContainAllExpectedValues() {
        GroupOperator[] values = GroupOperator.values();
        assertTrue(java.util.Arrays.asList(values).contains(GroupOperator.AND));
        assertTrue(java.util.Arrays.asList(values).contains(GroupOperator.OR));
    }

    @Test
    void shouldThrowExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () ->
                GroupOperator.valueOf("INVALID"));
    }
}
