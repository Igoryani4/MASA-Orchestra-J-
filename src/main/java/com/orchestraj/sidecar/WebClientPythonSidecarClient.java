package com.orchestraj.sidecar;

import com.orchestraj.agent.Goal;
import com.orchestraj.dto.SidecarRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebClientPythonSidecarClient implements PythonSidecarClient {

    private final WebClient webClient;

    public WebClientPythonSidecarClient(@Value("${sidecar.base-url:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Mono<SidecarResult> runTask(Goal goal, String instructionJson) {
        var request = new SidecarRequest(goal.sessionId(), goal.id(), instructionJson);
        return webClient.post()
                .uri("/inference/run-task")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SidecarResult.class);
    }
}
