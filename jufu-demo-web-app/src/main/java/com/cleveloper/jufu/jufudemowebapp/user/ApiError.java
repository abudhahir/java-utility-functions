package com.cleveloper.jufu.jufudemowebapp.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API error response")
public class ApiError {
    @Schema(description = "Error code identifier", example = "USER_NOT_FOUND")
    private String code;

    @Schema(description = "Human-readable error message", example = "User with id u-101 not found")
    private String message;

    @Schema(description = "Request correlation ID for tracing", example = "corr-123456")
    private String correlationId;
}

