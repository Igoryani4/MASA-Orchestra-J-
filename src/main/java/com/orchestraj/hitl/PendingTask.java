package com.orchestraj.hitl;

import java.time.Instant;
import reactor.core.publisher.Sinks;

public record PendingTask(
        String taskId,
        String sessionId,
        String reason,
        TaskStatus status,
        Instant createdAt,
        Sinks.One<ApprovalDecision> sink
) {
    public PendingTask withStatus(TaskStatus newStatus) {
        return new PendingTask(taskId, sessionId, reason, newStatus, createdAt, sink);
    }
}
