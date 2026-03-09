package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class ConditionTest {

    @Test
    void shouldEvaluateCondition() {
        Condition condition = new TestCondition(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldReturnFailureWhenConditionFails() {
        Condition condition = new TestCondition(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
    }

    @Test
    void shouldWorkAsLambda() {
        Condition condition = ctx -> ConditionResult.success();
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldWorkAsMethodReference() {
        Condition condition = this::alwaysSuccessCondition;
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldReturnFailureWithDetails() {
        Condition condition = new TestCondition(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());
        assertEquals("Test condition failed", result.getFailures().get(0).getMessage());
    }

    @Test
    void shouldEvaluateBasedOnRequestContext() {
        Condition condition = ctx -> {
            String apiKey = ctx.getHeader("X-Api-Key");
            return apiKey != null && apiKey.equals("valid-key")
                ? ConditionResult.success()
                : ConditionResult.failure(
                    ConditionFailure.builder()
                        .message("Invalid API key")
                        .build()
                );
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "valid-key");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateFailureBasedOnRequestContext() {
        Condition condition = ctx -> {
            String apiKey = ctx.getHeader("X-Api-Key");
            return apiKey != null && apiKey.equals("valid-key")
                ? ConditionResult.success()
                : ConditionResult.failure(
                    ConditionFailure.builder()
                        .message("Invalid API key")
                        .build()
                );
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "invalid-key");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
    }

    private ConditionResult alwaysSuccessCondition(RequestContext context) {
        return ConditionResult.success();
    }

    // Test implementation
    private static class TestCondition implements Condition {
        private final boolean shouldMatch;

        TestCondition(boolean shouldMatch) {
            this.shouldMatch = shouldMatch;
        }

        @Override
        public ConditionResult evaluate(RequestContext context) {
            return shouldMatch ? ConditionResult.success()
                              : ConditionResult.failure(
                                  ConditionFailure.builder()
                                      .message("Test condition failed")
                                      .build()
                              );
        }
    }
}
