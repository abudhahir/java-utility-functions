package com.cleveloper.jufu.jufudemowebapp.api;

import com.cleveloper.jufu.jufudemowebapp.user.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientResponseException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiError> handleBackendError(RestClientResponseException e) {
        String correlationId = CorrelationIdFilter.getCorrelationId();
        int statusCode = e.getStatusCode().value();

        String code = mapStatusToErrorCode(statusCode);
        String message = extractErrorMessage(e, statusCode);

        ApiError error = ApiError.builder()
            .code(code)
            .message(message)
            .correlationId(correlationId)
            .build();

        log.error("Backend error {}: {} (corrId: {})", statusCode, message, correlationId);

        return ResponseEntity
            .status(statusCode)
            .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericError(Exception e) {
        String correlationId = CorrelationIdFilter.getCorrelationId();

        ApiError error = ApiError.builder()
            .code("INTERNAL_ERROR")
            .message("An unexpected error occurred")
            .correlationId(correlationId)
            .build();

        log.error("Unexpected error (corrId: {})", correlationId, e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error);
    }

    private String mapStatusToErrorCode(int statusCode) {
        return switch (statusCode) {
            case 404 -> "USER_NOT_FOUND";
            case 409 -> "EMAIL_CONFLICT";
            case 422 -> "INVALID_INPUT";
            case 400 -> "BAD_REQUEST";
            case 500 -> "INTERNAL_ERROR";
            default -> "UNKNOWN_ERROR";
        };
    }

    private String extractErrorMessage(RestClientResponseException e, int statusCode) {
        String responseBody = e.getResponseBodyAsString();
        try {
            return responseBody;
        } catch (Exception ex) {
            return switch (statusCode) {
                case 404 -> "Resource not found";
                case 409 -> "Resource conflict";
                case 422 -> "Invalid input data";
                case 400 -> "Bad request";
                default -> "An error occurred";
            };
        }
    }
}

