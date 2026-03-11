package com.cleveloper.jufu.jufudemowebapp.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A single search result item.
 * {@code fullContent} is only present for premium-tier responses;
 * basic-tier responses carry {@code null} and it is omitted from JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A single search result item")
public record SearchHit(
        @Schema(description = "Unique item identifier", example = "p-001")
        String id,

        @Schema(description = "Item title", example = "Spring Boot in Action")
        String title,

        @Schema(description = "Short excerpt shown in all tiers", example = "A comprehensive guide to Spring Boot.")
        String snippet,

        @Schema(description = "Full content — present for premium tier only")
        String fullContent
) {}
