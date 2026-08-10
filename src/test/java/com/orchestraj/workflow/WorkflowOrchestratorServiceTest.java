package com.orchestraj.workflow;

import com.orchestraj.agent.impl.CriticAgent;
import com.orchestraj.agent.impl.ExecutorAgent;
import com.orchestraj.hitl.HitlTaskService;
import com.orchestraj.memory.MemoryService;
import com.orchestraj.sidecar.PythonSidecarClient;
import com.orchestraj.sidecar.SidecarResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class WorkflowOrchestratorServiceTest {

    @Test
    void shouldCompleteWhenConfidenceIsEnough() {
        PythonSidecarClient sidecarClient = (goal, instructionJson) ->
                Mono.just(new SidecarResult(true, 0.9, "{\"ok\":true}", null));

        var service = new WorkflowOrchestratorService(
                new ExecutorAgent(sidecarClient),
                new CriticAgent(0.75),
                new HitlTaskService(),
                new MemoryService()
        );

        var request = new WorkflowRequest("s1", "g1", "test-goal", "biomed", "{\"task\":\"x\"}");
        StepVerifier.create(service.run(request))
                .expectNextMatches(status -> "COMPLETED".equals(status.state()))
                .verifyComplete();
    }
}
