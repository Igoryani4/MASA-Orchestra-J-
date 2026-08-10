package com.orchestraj.workflow;

import com.orchestraj.agent.impl.CriticAgent;
import com.orchestraj.agent.impl.ExecutorAgent;
import com.orchestraj.hitl.HitlTaskService;
import com.orchestraj.memory.MemoryService;
import com.orchestraj.sidecar.PythonSidecarClient;
import com.orchestraj.sidecar.SidecarResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncWorkflowTaskServiceTest {

    @Test
    void shouldRunWorkflowAsynchronouslyAndExposeStatus() throws InterruptedException {
        PythonSidecarClient sidecarClient = (goal, instructionJson) ->
                Mono.delay(Duration.ofMillis(30))
                        .thenReturn(new SidecarResult(true, 0.95, "{\"ok\":true}", null));

        var orchestrator = new WorkflowOrchestratorService(
                new ExecutorAgent(sidecarClient),
                new CriticAgent(0.75),
                new HitlTaskService(),
                new MemoryService()
        );
        var asyncService = new AsyncWorkflowTaskService(orchestrator, 1);
        var request = new WorkflowRequest("s1", "g1", "test-goal", "biomed", "{\"task\":\"x\"}");

        AsyncWorkflowTaskStatus created = asyncService.start(request);
        assertNotNull(created.taskId());
        assertTrue(created.state() == AsyncWorkflowState.PENDING || created.state() == AsyncWorkflowState.RUNNING);

        Thread.sleep(80);
        AsyncWorkflowTaskStatus completed = asyncService.getStatus(created.taskId());
        assertEquals(AsyncWorkflowState.COMPLETED, completed.state());
        assertNotNull(completed.result());
        assertEquals("COMPLETED", completed.result().state());
    }

    @Test
    void shouldCancelRunningWorkflow() {
        PythonSidecarClient sidecarClient = (goal, instructionJson) ->
                Mono.<SidecarResult>never();

        var orchestrator = new WorkflowOrchestratorService(
                new ExecutorAgent(sidecarClient),
                new CriticAgent(0.75),
                new HitlTaskService(),
                new MemoryService()
        );
        var asyncService = new AsyncWorkflowTaskService(orchestrator, 5);
        var request = new WorkflowRequest("s1", "g1", "test-goal", "biomed", "{\"task\":\"x\"}");

        AsyncWorkflowTaskStatus created = asyncService.start(request);
        AsyncWorkflowTaskStatus canceled = asyncService.cancel(created.taskId());
        assertEquals(AsyncWorkflowState.CANCELED, canceled.state());
        assertEquals("Canceled by user", canceled.error());
    }

    @Test
    void shouldMarkWorkflowAsTimedOut() throws InterruptedException {
        PythonSidecarClient sidecarClient = (goal, instructionJson) ->
                Mono.delay(Duration.ofMillis(120))
                        .thenReturn(new SidecarResult(true, 0.95, "{\"ok\":true}", null));

        var orchestrator = new WorkflowOrchestratorService(
                new ExecutorAgent(sidecarClient),
                new CriticAgent(0.75),
                new HitlTaskService(),
                new MemoryService()
        );
        var asyncService = new AsyncWorkflowTaskService(orchestrator, 0);
        var request = new WorkflowRequest("s1", "g1", "test-goal", "biomed", "{\"task\":\"x\"}");

        AsyncWorkflowTaskStatus created = asyncService.start(request);
        Thread.sleep(30);

        AsyncWorkflowTaskStatus timedOut = asyncService.getStatus(created.taskId());
        assertEquals(AsyncWorkflowState.TIMED_OUT, timedOut.state());
        assertNotNull(timedOut.error());
    }
}
