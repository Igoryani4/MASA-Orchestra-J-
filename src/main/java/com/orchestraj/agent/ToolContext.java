package com.orchestraj.agent;

public record ToolContext<T extends Tool>(Goal goal, T tool, String instructionJson) {
}
