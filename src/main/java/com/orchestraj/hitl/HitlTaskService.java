package com.orchestraj.hitl;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
public class HitlTaskService {
    private final Map<String, PendingTask> tasks = new ConcurrentHashMap<>();

    public PendingTask create(String sessionId, String reason) {
        String id = UUID.randomUUID().toString();
        Sinks.One<ApprovalDecision> sink = Sinks.one();
        PendingTask task = new PendingTask(id, sessionId, reason, TaskStatus.PENDING_APPROVAL, Instant.now(), sink);
        tasks.put(id, task);
        return task;
    }

    public Mono<ApprovalDecision> waitForDecision(String taskId) {
        PendingTask task = tasks.get(taskId);
        if (task == null) {
            return Mono.error(new IllegalArgumentException("Unknown HITL task: " + taskId));
        }
        return task.sink().asMono();
    }

    public PendingTask approve(String taskId, boolean approved, String comment) {
        PendingTask task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Unknown HITL task: " + taskId);
        }
        task.sink().tryEmitValue(new ApprovalDecision(approved, comment));
        TaskStatus status = approved ? TaskStatus.APPROVED : TaskStatus.REJECTED;
        PendingTask updated = task.withStatus(status);
        tasks.put(taskId, updated);
        return updated;
    }
}
