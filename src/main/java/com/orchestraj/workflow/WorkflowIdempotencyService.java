package com.orchestraj.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WorkflowIdempotencyService {
    private final Map<String, SyncEntry> syncExecutions = new ConcurrentHashMap<>();
    private final Map<String, AsyncEntry> asyncExecutions = new ConcurrentHashMap<>();

    public Mono<WorkflowStatus> runSync(String idempotencyKey, WorkflowRequest request, Supplier<Mono<WorkflowStatus>> launch) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return launch.get();
        }
        String fingerprint = fingerprint(request);
        SyncEntry entry = syncExecutions.compute(idempotencyKey, (key, existing) -> {
            if (existing == null) {
                return new SyncEntry(fingerprint, launch.get().cache());
            }
            validateFingerprint(idempotencyKey, existing.fingerprint(), fingerprint);
            return existing;
        });
        return entry.execution();
    }

    public String resolveAsyncTaskId(String idempotencyKey, WorkflowRequest request, Supplier<String> launch) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return launch.get();
        }
        String fingerprint = fingerprint(request);
        AsyncEntry existing = asyncExecutions.get(idempotencyKey);
        if (existing != null) {
            validateFingerprint(idempotencyKey, existing.fingerprint(), fingerprint);
            return existing.taskId();
        }
        AsyncEntry created = asyncExecutions.computeIfAbsent(
                idempotencyKey,
                key -> new AsyncEntry(fingerprint, launch.get())
        );
        validateFingerprint(idempotencyKey, created.fingerprint(), fingerprint);
        return created.taskId();
    }

    private String fingerprint(WorkflowRequest request) {
        return request.sessionId() + "|" + request.goalId() + "|" + request.objective() + "|"
                + request.domain() + "|" + request.instructionJson();
    }

    private void validateFingerprint(String key, String existingFingerprint, String requestedFingerprint) {
        if (!existingFingerprint.equals(requestedFingerprint)) {
            throw new IllegalArgumentException("Idempotency key already used for a different workflow request: " + key);
        }
    }

    private record SyncEntry(String fingerprint, Mono<WorkflowStatus> execution) {
    }

    private record AsyncEntry(String fingerprint, String taskId) {
    }
}
