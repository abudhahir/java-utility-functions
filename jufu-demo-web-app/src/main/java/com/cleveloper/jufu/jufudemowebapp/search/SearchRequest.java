package com.cleveloper.jufu.jufudemowebapp.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for the tiered search endpoint.
 * The subscription tier is supplied via the {@code X-Subscription-Tier} header,
 * not the body, so this record carries only the search query.
 */
@Schema(description = "Search request payload")
public record SearchRequest(
        @Schema(description = "Search query string", example = "spring boot")
        String query
) {}
