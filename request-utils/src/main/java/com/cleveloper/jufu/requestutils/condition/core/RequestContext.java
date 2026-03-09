package com.cleveloper.jufu.requestutils.condition.core;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Framework-agnostic abstraction over HTTP request.
 * Provides access to headers, query parameters, and body for condition evaluation.
 */
public interface RequestContext {

    /**
     * Get the first value of a header, or null if not present.
     */
    String getHeader(String name);

    /**
     * Get all values of a header.
     */
    List<String> getHeaders(String name);

    /**
     * Get the first value of a query parameter, or null if not present.
     */
    String getQueryParam(String name);

    /**
     * Get all values of a query parameter.
     */
    List<String> getQueryParams(String name);

    /**
     * Get the raw request body as a string, or null if no body.
     */
    String getBody();

    /**
     * Get the parsed JSON body, or null if body is not JSON.
     * Lazy-parsed and cached.
     */
    Object getJsonBody();

    /**
     * Create a RequestContext from a Spring HttpServletRequest.
     */
    static RequestContext from(HttpServletRequest request) {
        return new RequestContextImpl(request);
    }
}
