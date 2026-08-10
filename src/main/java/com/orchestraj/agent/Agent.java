package com.orchestraj.agent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Agent<T extends Tool, R extends Result> {
    Flux<AgentStep> plan(Goal goal);

    Mono<R> execute(ToolContext<T> context);

    Mono<Correction> selfCorrect(AgentError error);
}
