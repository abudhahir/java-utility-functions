package com.cleveloper.jufu.requestutils.condition.core;

import com.cleveloper.jufu.requestutils.condition.matchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class RequestConditionMatcherTest {

    private final RequestConditionMatcher matcher = new RequestConditionMatcher();

    @Test
    void shouldEvaluateSingleCondition() {
        Condition condition = new HeaderCondition("X-Api-Key", "test", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "test");

        ConditionResult result = matcher.evaluate(condition, request);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldEvaluateConditionGroup() {
        ConditionGroup group = ConditionGroup.builder()
            .and(new HeaderCondition("X-Api-Key", "test", MatchOperation.EQUALS, false))
            .and(new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false))
            .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "test");
        request.addParameter("userId", "123");

        ConditionResult result = matcher.evaluate(group, request);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldReturnFailuresWhenConditionFails() {
        Condition condition = new HeaderCondition("X-Api-Key", "expected", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "actual");

        ConditionResult result = matcher.evaluate(condition, request);

        assertFalse(result.isMatched());
        assertFalse(result.getFailures().isEmpty());
    }
}
