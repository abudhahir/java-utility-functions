package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class QueryParamConditionTest {

    @Test
    void shouldMatchWhenQueryParamEquals() {
        QueryParamCondition condition = new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }

    @Test
    void shouldFailWhenQueryParamDoesNotMatch() {
        QueryParamCondition condition = new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "456");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals("QueryParam", result.getFailures().get(0).getConditionType());
    }

    @Test
    void shouldFailWhenQueryParamMissing() {
        QueryParamCondition condition = new QueryParamCondition("userId", "123", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertTrue(result.getFailures().get(0).getMessage().contains("missing"));
    }
}
