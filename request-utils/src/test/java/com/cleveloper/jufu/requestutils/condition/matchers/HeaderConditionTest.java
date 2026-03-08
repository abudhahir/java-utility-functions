package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;

class HeaderConditionTest {

    @Test
    void shouldMatchWhenHeaderEquals() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "premium", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldFailWhenHeaderDoesNotMatch() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "premium", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "basic");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());

        ConditionFailure failure = result.getFailures().get(0);
        assertEquals("Header", failure.getConditionType());
        assertEquals("X-Api-Key", failure.getFieldName());
        assertTrue(failure.getMessage().contains("premium"));
        assertTrue(failure.getMessage().contains("basic"));
    }

    @Test
    void shouldFailWhenHeaderMissing() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "premium", MatchOperation.EQUALS, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertTrue(result.getFailures().get(0).getMessage().contains("missing"));
    }

    @Test
    void shouldMatchCaseInsensitive() {
        HeaderCondition condition = new HeaderCondition("X-Api-Key", "PREMIUM", MatchOperation.EQUALS, true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "premium");
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
    }
}
