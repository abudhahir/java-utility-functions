package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationModeTest {

    @Test
    void shouldHaveFailFastMode() {
        assertNotNull(EvaluationMode.FAIL_FAST);
    }

    @Test
    void shouldHaveCollectAllMode() {
        assertNotNull(EvaluationMode.COLLECT_ALL);
    }

    @Test
    void shouldHaveExactlyTwoValues() {
        assertEquals(2, EvaluationMode.values().length);
    }

    @Test
    void shouldConvertFromString() {
        assertEquals(EvaluationMode.FAIL_FAST, EvaluationMode.valueOf("FAIL_FAST"));
        assertEquals(EvaluationMode.COLLECT_ALL, EvaluationMode.valueOf("COLLECT_ALL"));
    }

    @Test
    void shouldContainAllExpectedValues() {
        EvaluationMode[] values = EvaluationMode.values();
        assertTrue(java.util.Arrays.asList(values).contains(EvaluationMode.FAIL_FAST));
        assertTrue(java.util.Arrays.asList(values).contains(EvaluationMode.COLLECT_ALL));
    }

    @Test
    void shouldThrowExceptionForInvalidValue() {
        assertThrows(IllegalArgumentException.class, () ->
                EvaluationMode.valueOf("INVALID"));
    }
}
