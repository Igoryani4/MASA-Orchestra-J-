package com.orchestraj.agent.impl;

import com.orchestraj.agent.Agent;
import com.orchestraj.agent.AgentError;
import com.orchestraj.agent.AgentStep;
import com.orchestraj.agent.Correction;
import com.orchestraj.agent.Goal;
import com.orchestraj.agent.ToolContext;
import com.orchestraj.sidecar.SidecarResult;
import com.orchestraj.sidecar.SidecarTool;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PlannerAgent implements Agent<SidecarTool, SidecarResult> {
    @Override
    public Flux<AgentStep> plan(Goal goal) {
        return Flux.just(
                new AgentStep(1, "Validate and enrich objective"),
                new AgentStep(2, "Request sidecar inference"),
                new AgentStep(3, "Critic validation"),
                new AgentStep(4, "Publish workflow result")
        );
    }

    @Override
    public Mono<SidecarResult> execute(ToolContext<SidecarTool> context) {
        return Mono.just(new SidecarResult(true, 1.0, context.instructionJson(), null));
    }

    @Override
    public Mono<Correction> selfCorrect(AgentError error) {
        return Mono.just(new Correction(error.code(), "Re-plan workflow with fallback policy"));
    }
}
