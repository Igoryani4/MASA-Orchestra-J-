package com.orchestraj.api;

import com.orchestraj.workflow.AsyncWorkflowTaskService;
import com.orchestraj.workflow.AsyncWorkflowTaskStatus;
import com.orchestraj.workflow.WorkflowIdempotencyService;
import com.orchestraj.workflow.WorkflowOrchestratorService;
import com.orchestraj.workflow.WorkflowRequest;
import com.orchestraj.workflow.WorkflowStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    private final WorkflowOrchestratorService orchestratorService;
    private final AsyncWorkflowTaskService asyncWorkflowTaskService;
    private final WorkflowIdempotencyService workflowIdempotencyService;

    public WorkflowController(
            WorkflowOrchestratorService orchestratorService,
            AsyncWorkflowTaskService asyncWorkflowTaskService,
            WorkflowIdempotencyService workflowIdempotencyService
    ) {
        this.orchestratorService = orchestratorService;
        this.asyncWorkflowTaskService = asyncWorkflowTaskService;
        this.workflowIdempotencyService = workflowIdempotencyService;
    }

    @PostMapping("/run")
    public Mono<WorkflowStatus> run(
            @Valid @RequestBody WorkflowRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        try {
            return workflowIdempotencyService.runSync(idempotencyKey, request, () -> orchestratorService.run(request));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @PostMapping("/run-async")
    public AsyncWorkflowTaskStatus runAsync(
            @Valid @RequestBody WorkflowRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        try {
            String taskId = workflowIdempotencyService.resolveAsyncTaskId(
                    idempotencyKey,
                    request,
                    () -> asyncWorkflowTaskService.start(request).taskId()
            );
            return asyncWorkflowTaskService.getStatus(taskId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping("/{taskId}")
    public AsyncWorkflowTaskStatus taskStatus(@PathVariable String taskId) {
        try {
            return asyncWorkflowTaskService.getStatus(taskId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/{taskId}/cancel")
    public AsyncWorkflowTaskStatus cancel(@PathVariable String taskId) {
        try {
            return asyncWorkflowTaskService.cancel(taskId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
