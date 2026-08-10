package com.orchestraj.api;

import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.server.reactive.ServerHttpRequest;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception,
            ServerHttpRequest request
    ) {
        String reason = exception.getReason() == null ? "Request failed" : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiError(
                        "REQUEST_FAILED",
                        reason,
                        null,
                        resolveTraceId(request)
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException exception,
            ServerHttpRequest request
    ) {
        String details = exception.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Constraint validation failed");
        return ResponseEntity.badRequest()
                .body(new ApiError(
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        details,
                        resolveTraceId(request)
                ));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiError> handleBindException(
            WebExchangeBindException exception,
            ServerHttpRequest request
    ) {
        String details = exception.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Request body validation failed");
        return ResponseEntity.badRequest()
                .body(new ApiError(
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        details,
                        resolveTraceId(request)
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception,
            ServerHttpRequest request
    ) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        "INVALID_ARGUMENT",
                        exception.getMessage(),
                        null,
                        resolveTraceId(request)
                ));
    }

    private String resolveTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Correlation-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeaders().getFirst("X-Trace-Id");
        }
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }
}
