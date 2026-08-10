package com.orchestraj.dto;

public record SidecarRequest(String sessionId, String goalId, String instructionJson) {
}
