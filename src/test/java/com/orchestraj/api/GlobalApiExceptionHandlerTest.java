package com.orchestraj.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalApiExceptionHandlerTest {

    @Test
    void shouldReturnUnifiedErrorForResponseStatusException() {
        var handler = new GlobalApiExceptionHandler();
        var request = MockServerHttpRequest.get("/api/workflow/unknown")
                .header("X-Correlation-Id", "trace-1")
                .build();

        var response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"),
                request
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("REQUEST_FAILED", response.getBody().code());
        assertEquals("Not found", response.getBody().message());
        assertEquals("trace-1", response.getBody().traceId());
    }
}
