package com.orchestraj.workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

@Service
public class AsyncWorkflowTaskService {
    private final WorkflowOrchestratorService orchestratorService;
    private final Duration timeout;
    private final Map<String, StoredTask> tasks = new ConcurrentHashMap<>();
    private final Map<String, Disposable> runningTasks = new ConcurrentHashMap<>();

    public AsyncWorkflowTaskService(
            WorkflowOrchestratorService orchestratorService,
            @Value("${workflow.async.timeout-seconds:30}") long timeoutSeconds
    ) {
        this.orchestratorService = orchestratorService;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public AsyncWorkflowTaskStatus start(WorkflowRequest request) {
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        tasks.put(taskId, new StoredTask(
                taskId,
                AsyncWorkflowState.PENDING,
                now,
                now,
                null,
                null
        ));

        Disposable subscription = orchestratorService.run(request)
                .timeout(timeout)
                .doOnSubscribe(disposable -> update(taskId, AsyncWorkflowState.RUNNING, null, null))
                .subscribe(
                        result -> {
                            update(taskId, AsyncWorkflowState.COMPLETED, result, null);
                            runningTasks.remove(taskId);
                        },
                        error -> {
                            AsyncWorkflowState state = error instanceof TimeoutException
                                    ? AsyncWorkflowState.TIMED_OUT
                                    : AsyncWorkflowState.FAILED;
                            update(taskId, state, null, error.getMessage());
                            runningTasks.remove(taskId);
                        }
                );
        if (!subscription.isDisposed()) {
            runningTasks.put(taskId, subscription);
        }

        return toStatus(tasks.get(taskId));
    }

    public AsyncWorkflowTaskStatus getStatus(String taskId) {
        StoredTask task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Unknown workflow task: " + taskId);
        }
        return toStatus(task);
    }

    public AsyncWorkflowTaskStatus cancel(String taskId) {
        StoredTask task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Unknown workflow task: " + taskId);
        }
        if (task.state() == AsyncWorkflowState.COMPLETED
                || task.state() == AsyncWorkflowState.FAILED
                || task.state() == AsyncWorkflowState.TIMED_OUT
                || task.state() == AsyncWorkflowState.CANCELED) {
            return toStatus(task);
        }
        Disposable subscription = runningTasks.remove(taskId);
        if (subscription != null) {
            subscription.dispose();
        }
        update(taskId, AsyncWorkflowState.CANCELED, null, "Canceled by user");
        return getStatus(taskId);
    }

    private void update(String taskId, AsyncWorkflowState state, WorkflowStatus result, String error) {
        tasks.computeIfPresent(taskId, (id, current) -> new StoredTask(
                current.taskId(),
                state,
                current.createdAt(),
                Instant.now(),
                result,
                error
        ));
    }

    private AsyncWorkflowTaskStatus toStatus(StoredTask task) {
        return new AsyncWorkflowTaskStatus(
                task.taskId(),
                task.state(),
                task.createdAt(),
                task.updatedAt(),
                task.result(),
                task.error()
        );
    }

    private record StoredTask(
            String taskId,
            AsyncWorkflowState state,
            Instant createdAt,
            Instant updatedAt,
            WorkflowStatus result,
            String error
    ) {
    }
}
