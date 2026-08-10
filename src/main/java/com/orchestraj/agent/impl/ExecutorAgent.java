package com.orchestraj.agent.impl;

import com.orchestraj.agent.Agent;
import com.orchestraj.agent.AgentError;
import com.orchestraj.agent.AgentStep;
import com.orchestraj.agent.Correction;
import com.orchestraj.agent.Goal;
import com.orchestraj.agent.ToolContext;
import com.orchestraj.sidecar.PythonSidecarClient;
import com.orchestraj.sidecar.SidecarResult;
import com.orchestraj.sidecar.SidecarTool;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ExecutorAgent implements Agent<SidecarTool, SidecarResult> {
    private final PythonSidecarClient sidecarClient;

    public ExecutorAgent(PythonSidecarClient sidecarClient) {
        this.sidecarClient = sidecarClient;
    }

    @Override
    public Flux<AgentStep> plan(Goal goal) {
        return Flux.just(new AgentStep(1, "Execute sidecar task"));
    }

    @Override
    public Mono<SidecarResult> execute(ToolContext<SidecarTool> context) {
        return sidecarClient.runTask(context.goal(), context.instructionJson());
    }

    @Override
    public Mono<Correction> selfCorrect(AgentError error) {
        return Mono.just(new Correction(error.code(), "Route to HITL queue"));
    }
}
