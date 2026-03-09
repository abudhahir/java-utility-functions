package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of RequestContext backed by Spring's HttpServletRequest.
 */
class RequestContextImpl implements RequestContext {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpServletRequest request;
    private String body;
    private Object jsonBody;
    private boolean jsonParsed = false;

    RequestContextImpl(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        return Collections.list(request.getHeaders(name));
    }

    @Override
    public String getQueryParam(String name) {
        return request.getParameter(name);
    }

    @Override
    public List<String> getQueryParams(String name) {
        String[] values = request.getParameterValues(name);
        return values != null ? List.of(values) : Collections.emptyList();
    }

    @Override
    public String getBody() {
        if (body == null) {
            body = readBody();
        }
        return body;
    }

    @Override
    public Object getJsonBody() {
        if (!jsonParsed) {
            jsonParsed = true;
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains("application/json")) {
                String bodyContent = getBody();
                if (bodyContent != null) {
                    try {
                        jsonBody = OBJECT_MAPPER.readTree(bodyContent);
                    } catch (Exception e) {
                        // If parsing fails, leave jsonBody as null
                    }
                }
            }
        }
        return jsonBody;
    }

    private String readBody() {
        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
