package com.cleveloper.jufu.requestutils.condition.core;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class RequestContextImplTest {

    @Test
    void shouldParseJsonBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"name\":\"John\",\"age\":30}".getBytes());

        RequestContext context = RequestContext.from(request);
        Object jsonBody = context.getJsonBody();

        assertNotNull(jsonBody);
        assertTrue(jsonBody instanceof JsonNode);
        JsonNode node = (JsonNode) jsonBody;
        assertEquals("John", node.get("name").asText());
        assertEquals(30, node.get("age").asInt());
    }

    @Test
    void shouldReturnNullForNonJsonContent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("text/plain");
        request.setContent("plain text".getBytes());

        RequestContext context = RequestContext.from(request);
        Object jsonBody = context.getJsonBody();

        assertNull(jsonBody);
    }

    @Test
    void shouldCacheJsonBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"cached\":true}".getBytes());

        RequestContext context = RequestContext.from(request);
        Object jsonBody1 = context.getJsonBody();
        Object jsonBody2 = context.getJsonBody();

        assertNotNull(jsonBody1);
        assertSame(jsonBody1, jsonBody2);
    }
}
