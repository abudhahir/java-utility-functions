package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class JsonPathConditionTest {

    @Test
    void shouldMatchJsonPath() {
        JsonPathCondition condition = new JsonPathCondition(
            "$.user.type",
            "premium",
            MatchOperation.EQUALS,
            false
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"user\": {\"type\": \"premium\", \"id\": 123}}".getBytes(StandardCharsets.UTF_8));
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldFailWhenPathNotFound() {
        JsonPathCondition condition = new JsonPathCondition(
            "$.user.region",
            "US",
            MatchOperation.EQUALS,
            false
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"user\": {\"type\": \"premium\"}}".getBytes(StandardCharsets.UTF_8));
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());

        ConditionFailure failure = result.getFailures().get(0);
        assertEquals("JsonPath", failure.getConditionType());
        assertEquals("$.user.region", failure.getFieldName());
        assertTrue(failure.getMessage().contains("not found") || failure.getMessage().contains("missing"));
    }

    @Test
    void shouldFailWhenJsonBodyNull() {
        JsonPathCondition condition = new JsonPathCondition(
            "$.user.type",
            "premium",
            MatchOperation.EQUALS,
            false
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        // No content type or body set
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());

        ConditionFailure failure = result.getFailures().get(0);
        assertTrue(failure.getMessage().contains("JSON body") || failure.getMessage().contains("null"));
    }

    @Test
    void shouldSupportArrayAccess() {
        JsonPathCondition condition = new JsonPathCondition(
            "$.items[0].name",
            "widget",
            MatchOperation.EQUALS,
            false
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"items\": [{\"name\": \"widget\", \"price\": 10}]}".getBytes(StandardCharsets.UTF_8));
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }
}
