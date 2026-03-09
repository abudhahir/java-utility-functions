package com.cleveloper.jufu.requestutils.condition.matchers;

import com.cleveloper.jufu.requestutils.condition.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class JsonExactMatchConditionTest {

    @Test
    void shouldMatchExactFields() {
        String template = "{\"type\": \"premium\", \"region\": \"US\"}";
        String[] fields = {"type", "region"};
        JsonExactMatchCondition condition = new JsonExactMatchCondition(template, fields);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"type\": \"premium\", \"region\": \"US\", \"userId\": 123}".getBytes(StandardCharsets.UTF_8));
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertTrue(result.isMatched());
        assertTrue(result.getFailures().isEmpty());
    }

    @Test
    void shouldFailWhenFieldDoesNotMatch() {
        String template = "{\"type\": \"premium\", \"region\": \"US\"}";
        String[] fields = {"type", "region"};
        JsonExactMatchCondition condition = new JsonExactMatchCondition(template, fields);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"type\": \"basic\", \"region\": \"US\"}".getBytes(StandardCharsets.UTF_8));
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());

        ConditionFailure failure = result.getFailures().get(0);
        assertEquals("JsonExactMatch", failure.getConditionType());
        assertEquals("type", failure.getFieldName());
        assertTrue(failure.getMessage().contains("premium"));
        assertTrue(failure.getMessage().contains("basic"));
    }

    @Test
    void shouldFailWhenFieldMissing() {
        String template = "{\"type\": \"premium\", \"region\": \"US\"}";
        String[] fields = {"type", "region"};
        JsonExactMatchCondition condition = new JsonExactMatchCondition(template, fields);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"type\": \"premium\"}".getBytes(StandardCharsets.UTF_8));
        RequestContext context = RequestContext.from(request);

        ConditionResult result = condition.evaluate(context);

        assertFalse(result.isMatched());
        assertEquals(1, result.getFailures().size());

        ConditionFailure failure = result.getFailures().get(0);
        assertTrue(failure.getMessage().contains("missing") || failure.getMessage().contains("not found"));
    }
}
