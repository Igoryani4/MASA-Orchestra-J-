package com.orchestraj.api;

import com.orchestraj.workflow.WorkflowOrchestratorService;
import com.orchestraj.workflow.WorkflowRequest;
import com.orchestraj.workflow.WorkflowStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    private final WorkflowOrchestratorService orchestratorService;

    public WorkflowController(WorkflowOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/run")
    public Mono<WorkflowStatus> run(@Valid @RequestBody WorkflowRequest request) {
        return orchestratorService.run(request);
    }
}
