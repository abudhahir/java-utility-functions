package com.cleveloper.jufu.requestutils.condition.aop;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ConditionNotMetExceptionTest {

    @Test
    void shouldCreateExceptionWithResult() {
        ConditionFailure failure = ConditionFailure.builder()
            .conditionType("Header")
            .message("Test failure")
            .build();
        ConditionResult result = ConditionResult.failure(failure);

        ConditionNotMetException exception = new ConditionNotMetException(result);

        assertEquals(result, exception.getResult());
        assertTrue(exception.getMessage().contains("Test failure"));
    }

    @Test
    void shouldFormatMessageWithMultipleFailures() {
        ConditionFailure failure1 = ConditionFailure.builder()
            .conditionType("Header")
            .message("Failure 1")
            .build();
        ConditionFailure failure2 = ConditionFailure.builder()
            .conditionType("QueryParam")
            .message("Failure 2")
            .build();
        ConditionResult result = ConditionResult.failure(List.of(failure1, failure2));

        ConditionNotMetException exception = new ConditionNotMetException(result);

        String message = exception.getMessage();
        assertTrue(message.contains("Failure 1"));
        assertTrue(message.contains("Failure 2"));
    }
}
