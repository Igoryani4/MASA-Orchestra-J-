package com.orchestraj.hitl;

import java.time.Instant;

public record HitlTaskView(
        String taskId,
        String sessionId,
        String reason,
        TaskStatus status,
        Instant createdAt
) {
    public static HitlTaskView from(PendingTask task) {
        return new HitlTaskView(
                task.taskId(),
                task.sessionId(),
                task.reason(),
                task.status(),
                task.createdAt()
        );
    }
}
