package com.orchestraj.sidecar;

import com.orchestraj.agent.Result;

public record SidecarResult(boolean success, double confidence, String payload, String error) implements Result {
}
