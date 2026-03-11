package com.cleveloper.jufu.jufudemowebapp.search;

import com.cleveloper.jufu.requestutils.condition.core.ConditionFailure;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Uniform response envelope for the search endpoint.
 *
 * <p>Success (premium):
 * <pre>
 * { "query": "spring", "tier": "premium", "premium": true, "hits": [...] }
 * </pre>
 *
 * <p>Success (basic):
 * <pre>
 * { "query": "spring", "tier": "basic", "premium": false, "hits": [...],
 *   "upgradeHint": "Upgrade to premium for full content access" }
 * </pre>
 *
 * <p>Invalid request:
 * <pre>
 * { "errorCode": "INVALID_REQUEST", "violations": ["Header 'X-Subscription-Tier' is missing"] }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Search response envelope")
public record SearchResponse(
        @Schema(description = "Echo of the search query")
        String query,

        @Schema(description = "Subscription tier used to resolve results", example = "premium")
        String tier,

        @Schema(description = "True when full content is included in hits")
        Boolean premium,

        @Schema(description = "Matched search results")
        List<SearchHit> hits,

        @Schema(description = "Present for basic-tier responses; absent for premium")
        String upgradeHint,

        @Schema(description = "Error code — present only when request validation fails", example = "INVALID_REQUEST")
        String errorCode,

        @Schema(description = "Per-field validation violations — present only on INVALID_REQUEST")
        List<String> violations
) {

    /** Full-content response for premium-tier callers. */
    public static SearchResponse full(String query, List<SearchHit> hits) {
        return new SearchResponse(query, "premium", true, hits, null, null, null);
    }

    /** Snippet-only response for basic-tier callers. */
    public static SearchResponse limited(String query, List<SearchHit> hits) {
        return new SearchResponse(query, "basic", false, hits,
                "Upgrade to premium for full content access", null, null);
    }

    /** Validation-failure response built from request-utils ConditionFailure list. */
    public static SearchResponse invalid(List<ConditionFailure> failures) {
        List<String> violations = failures.stream()
                .map(ConditionFailure::getMessage)
                .toList();
        return new SearchResponse(null, null, null, null, null, "INVALID_REQUEST", violations);
    }

    /** Validation-failure response for a blank query field. */
    public static SearchResponse blankQuery() {
        return new SearchResponse(null, null, null, null, null,
                "INVALID_REQUEST", List.of("query must not be blank"));
    }
}
