package com.orchestraj.workflow;

import com.orchestraj.agent.Goal;
import com.orchestraj.agent.ToolContext;
import com.orchestraj.agent.impl.CriticAgent;
import com.orchestraj.agent.impl.ExecutorAgent;
import com.orchestraj.hitl.HitlTaskService;
import com.orchestraj.memory.MemoryService;
import com.orchestraj.sidecar.SidecarTool;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class WorkflowOrchestratorService {
    private final ExecutorAgent executorAgent;
    private final CriticAgent criticAgent;
    private final HitlTaskService hitlTaskService;
    private final MemoryService memoryService;

    public WorkflowOrchestratorService(
            ExecutorAgent executorAgent,
            CriticAgent criticAgent,
            HitlTaskService hitlTaskService,
            MemoryService memoryService
    ) {
        this.executorAgent = executorAgent;
        this.criticAgent = criticAgent;
        this.hitlTaskService = hitlTaskService;
        this.memoryService = memoryService;
    }

    public Mono<WorkflowStatus> run(WorkflowRequest request) {
        Goal goal = new Goal(request.goalId(), request.sessionId(), request.objective(), request.domain());
        var context = new ToolContext<>(goal, new SidecarTool("inference"), request.instructionJson());
        memoryService.appendStep(request.sessionId(), "workflow-started");

        return executorAgent.execute(context)
                .flatMap(result -> {
                    var decision = criticAgent.evaluate(result);
                    if (decision.accepted()) {
                        memoryService.appendStep(request.sessionId(), "workflow-accepted");
                        return Mono.just(new WorkflowStatus(
                                request.sessionId(),
                                request.goalId(),
                                "COMPLETED",
                                decision.reason(),
                                result.payload()
                        ));
                    }
                    var task = hitlTaskService.create(request.sessionId(), decision.reason());
                    memoryService.appendStep(request.sessionId(), "workflow-awaiting-hitl");
                    return hitlTaskService.waitForDecision(task.taskId())
                            .map(approval -> new WorkflowStatus(
                                    request.sessionId(),
                                    request.goalId(),
                                    approval.approved() ? "COMPLETED_AFTER_HITL" : "REJECTED_BY_HUMAN",
                                    approval.comment(),
                                    result.payload()
                            ));
                });
    }
}
