package com.orchestraj.sidecar;

import com.orchestraj.agent.Goal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebClientPythonSidecarClientTest {

    @Test
    void shouldRetryAndSucceedAfterTransientServerErrors() {
        AtomicInteger requestCount = new AtomicInteger(0);
        ExchangeFunction exchangeFunction = request -> {
            int attempt = requestCount.incrementAndGet();
            if (attempt < 3) {
                return reactor.core.publisher.Mono.just(
                        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()
                );
            }
            return reactor.core.publisher.Mono.just(
                    ClientResponse.create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body("{\"success\":true,\"confidence\":0.91,\"payload\":\"{}\",\"error\":null}")
                            .build()
            );
        };

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        var client = new WebClientPythonSidecarClient(webClient, 5000, 2, 1, 5, 10000);

        SidecarResult result = client.runTask(new Goal("g1", "s1", "o", "biomed"), "{\"task\":\"x\"}")
                .block(Duration.ofSeconds(2));

        assertEquals(3, requestCount.get());
        assertTrue(result.success());
    }

    @Test
    void shouldOpenCircuitAfterConfiguredFailures() {
        AtomicInteger requestCount = new AtomicInteger(0);
        ExchangeFunction exchangeFunction = request -> {
            requestCount.incrementAndGet();
            return reactor.core.publisher.Mono.just(
                    ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()
            );
        };

        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        var client = new WebClientPythonSidecarClient(webClient, 5000, 0, 1, 2, 10000);
        Goal goal = new Goal("g1", "s1", "o", "biomed");

        assertThrows(RuntimeException.class, () -> client.runTask(goal, "{\"task\":\"x\"}").block(Duration.ofSeconds(2)));
        assertThrows(RuntimeException.class, () -> client.runTask(goal, "{\"task\":\"x\"}").block(Duration.ofSeconds(2)));
        IllegalStateException circuitOpenError = assertThrows(
                IllegalStateException.class,
                () -> client.runTask(goal, "{\"task\":\"x\"}").block(Duration.ofSeconds(2))
        );

        assertEquals(2, requestCount.get());
        assertEquals("Sidecar circuit is open", circuitOpenError.getMessage());
    }
}
