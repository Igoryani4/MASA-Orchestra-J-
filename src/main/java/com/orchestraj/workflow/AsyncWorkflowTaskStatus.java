package com.orchestraj.workflow;

import java.time.Instant;

public record AsyncWorkflowTaskStatus(
        String taskId,
        AsyncWorkflowState state,
        Instant createdAt,
        Instant updatedAt,
        WorkflowStatus result,
        String error
) {
}
