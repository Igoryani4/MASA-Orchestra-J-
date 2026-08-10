package com.orchestraj.workflow;

public record WorkflowStatus(
        String sessionId,
        String goalId,
        String state,
        String reason,
        String payload
) {
}
