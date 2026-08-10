package com.orchestraj.sidecar;

import com.orchestraj.agent.Goal;
import com.orchestraj.dto.SidecarRequest;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class WebClientPythonSidecarClient implements PythonSidecarClient {

    private final WebClient webClient;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Duration retryBackoff;
    private final int circuitFailureThreshold;
    private final Duration circuitOpenDuration;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenUntilMillis = new AtomicLong(0);

    public WebClientPythonSidecarClient(
            @Value("${sidecar.base-url:http://localhost:8000}") String baseUrl,
            @Value("${sidecar.resilience.request-timeout-millis:5000}") long requestTimeoutMillis,
            @Value("${sidecar.resilience.max-retries:2}") int maxRetries,
            @Value("${sidecar.resilience.retry-backoff-millis:200}") long retryBackoffMillis,
            @Value("${sidecar.resilience.circuit-breaker.failure-threshold:3}") int circuitFailureThreshold,
            @Value("${sidecar.resilience.circuit-breaker.open-millis:10000}") long circuitOpenMillis
    ) {
        this(
                WebClient.builder().baseUrl(baseUrl).build(),
                requestTimeoutMillis,
                maxRetries,
                retryBackoffMillis,
                circuitFailureThreshold,
                circuitOpenMillis
        );
    }

    WebClientPythonSidecarClient(
            WebClient webClient,
            long requestTimeoutMillis,
            int maxRetries,
            long retryBackoffMillis,
            int circuitFailureThreshold,
            long circuitOpenMillis
    ) {
        this.webClient = webClient;
        this.requestTimeout = Duration.ofMillis(requestTimeoutMillis);
        this.maxRetries = maxRetries;
        this.retryBackoff = Duration.ofMillis(retryBackoffMillis);
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenDuration = Duration.ofMillis(circuitOpenMillis);
    }

    @Override
    public Mono<SidecarResult> runTask(Goal goal, String instructionJson) {
        if (isCircuitOpen()) {
            return Mono.error(new IllegalStateException("Sidecar circuit is open"));
        }
        var request = new SidecarRequest(goal.sessionId(), goal.id(), instructionJson);
        return webClient.post()
                .uri("/inference/run-task")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SidecarResult.class)
                .timeout(requestTimeout)
                .retryWhen(Retry.backoff(maxRetries, retryBackoff)
                        .filter(this::isRetryable)
                        .onRetryExhaustedThrow((retrySpec, retrySignal) -> retrySignal.failure()))
                .doOnSuccess(result -> resetCircuit())
                .doOnError(this::registerFailure);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientRequestException || throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }
        return false;
    }

    private boolean isCircuitOpen() {
        long openedUntil = circuitOpenUntilMillis.get();
        if (openedUntil == 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < openedUntil) {
            return true;
        }
        circuitOpenUntilMillis.compareAndSet(openedUntil, 0);
        consecutiveFailures.set(0);
        return false;
    }

    private void resetCircuit() {
        consecutiveFailures.set(0);
        circuitOpenUntilMillis.set(0);
    }

    private void registerFailure(Throwable error) {
        if (error instanceof IllegalStateException && "Sidecar circuit is open".equals(error.getMessage())) {
            return;
        }
        int failureCount = consecutiveFailures.incrementAndGet();
        if (failureCount >= circuitFailureThreshold) {
            circuitOpenUntilMillis.set(System.currentTimeMillis() + circuitOpenDuration.toMillis());
        }
    }
}
