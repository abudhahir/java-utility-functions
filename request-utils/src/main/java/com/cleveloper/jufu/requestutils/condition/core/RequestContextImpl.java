package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of RequestContext backed by Spring's HttpServletRequest.
 */
class RequestContextImpl implements RequestContext {
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
            // JSON parsing will be implemented when JSONPath dependency is added
            // For now, return null
            jsonParsed = true;
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
