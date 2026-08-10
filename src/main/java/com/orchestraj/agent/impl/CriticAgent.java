package com.orchestraj.agent.impl;

import com.orchestraj.sidecar.SidecarResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CriticAgent {
    private final double confidenceThreshold;

    public CriticAgent(@Value("${hitl.thresholds.biomed-confidence:0.75}") double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public CriticDecision evaluate(SidecarResult result) {
        if (!result.success()) {
            return new CriticDecision(false, "Sidecar returned error: " + result.error());
        }
        if (result.confidence() < confidenceThreshold) {
            return new CriticDecision(false, "Confidence below threshold");
        }
        return new CriticDecision(true, "Accepted");
    }

    public record CriticDecision(boolean accepted, String reason) {
    }
}
