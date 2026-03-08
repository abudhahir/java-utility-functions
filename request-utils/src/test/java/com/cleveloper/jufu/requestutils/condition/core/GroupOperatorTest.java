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
}
