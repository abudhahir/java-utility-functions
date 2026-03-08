package com.cleveloper.jufu.requestutils.condition.core;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RequestContextTest {

    @Test
    void shouldExtractHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Api-Key", "test-key");
        request.addHeader("Accept", "application/json");

        RequestContext context = RequestContext.from(request);

        assertEquals("test-key", context.getHeader("X-Api-Key"));
        assertEquals("application/json", context.getHeader("Accept"));
        assertNull(context.getHeader("NonExistent"));
    }

    @Test
    void shouldExtractQueryParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");
        request.addParameter("type", "premium");

        RequestContext context = RequestContext.from(request);

        assertEquals("123", context.getQueryParam("userId"));
        assertEquals("premium", context.getQueryParam("type"));
        assertNull(context.getQueryParam("nonExistent"));
    }

    @Test
    void shouldHandleMultiValueHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom", "value1");
        request.addHeader("X-Custom", "value2");

        RequestContext context = RequestContext.from(request);
        List<String> values = context.getHeaders("X-Custom");

        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    void shouldReturnNullForMissingJsonWhenNoBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestContext context = RequestContext.from(request);

        assertNull(context.getJsonBody());
    }

    @Test
    void shouldHandleMultiValueQueryParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("tags", "java");
        request.addParameter("tags", "spring");

        RequestContext context = RequestContext.from(request);
        List<String> values = context.getQueryParams("tags");

        assertEquals(2, values.size());
        assertTrue(values.contains("java"));
        assertTrue(values.contains("spring"));
    }

    @Test
    void shouldReturnEmptyListForMissingQueryParams() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestContext context = RequestContext.from(request);
        List<String> values = context.getQueryParams("nonExistent");

        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForMissingHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestContext context = RequestContext.from(request);
        List<String> values = context.getHeaders("NonExistent");

        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    void shouldExtractBodyContent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{\"key\":\"value\"}".getBytes());

        RequestContext context = RequestContext.from(request);
        String body = context.getBody();

        assertNotNull(body);
        assertEquals("{\"key\":\"value\"}", body);
    }

    @Test
    void shouldReturnNullForEmptyBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        RequestContext context = RequestContext.from(request);

        assertNull(context.getBody());
    }

    @Test
    void shouldCacheBodyContent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("test content".getBytes());

        RequestContext context = RequestContext.from(request);
        String body1 = context.getBody();
        String body2 = context.getBody();

        assertSame(body1, body2);
    }

    @Test
    void shouldHandleHeadersCaseInsensitively() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Content-Type", "application/json");

        RequestContext context = RequestContext.from(request);

        assertEquals("application/json", context.getHeader("Content-Type"));
        assertEquals("application/json", context.getHeader("content-type"));
    }

    @Test
    void shouldHandleFirstValueForSingleQueryParam() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "123");

        RequestContext context = RequestContext.from(request);

        assertEquals("123", context.getQueryParam("userId"));
    }

    @Test
    void shouldReturnFirstValueForMultiValueQueryParam() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("priority", new String[]{"high", "medium"});

        RequestContext context = RequestContext.from(request);

        assertEquals("high", context.getQueryParam("priority"));
    }
}
