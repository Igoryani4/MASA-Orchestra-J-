package com.orchestraj.api;

public record ApiError(
        String code,
        String message,
        String details,
        String traceId
) {
}
