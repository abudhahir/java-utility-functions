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
}
