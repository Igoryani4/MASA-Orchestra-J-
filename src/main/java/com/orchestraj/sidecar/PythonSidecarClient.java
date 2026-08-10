package com.orchestraj.sidecar;

import com.orchestraj.agent.Goal;
import reactor.core.publisher.Mono;

public interface PythonSidecarClient {
    Mono<SidecarResult> runTask(Goal goal, String instructionJson);
}
