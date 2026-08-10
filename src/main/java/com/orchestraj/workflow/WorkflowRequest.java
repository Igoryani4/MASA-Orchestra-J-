package com.orchestraj.workflow;

import jakarta.validation.constraints.NotBlank;

public record WorkflowRequest(
        @NotBlank String sessionId,
        @NotBlank String goalId,
        @NotBlank String objective,
        @NotBlank String domain,
        @NotBlank String instructionJson
) {
}
