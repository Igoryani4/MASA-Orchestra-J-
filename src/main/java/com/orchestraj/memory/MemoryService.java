package com.orchestraj.memory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private final ConcurrentHashMap<String, SessionMemory> memories = new ConcurrentHashMap<>();

    public SessionMemory getOrCreate(String sessionId) {
        return memories.computeIfAbsent(sessionId, id -> new SessionMemory(Duration.ofMinutes(15), 20));
    }

    public void appendStep(String sessionId, String step) {
        getOrCreate(sessionId).append(step);
    }

    public List<String> getRecentSteps(String sessionId) {
        return getOrCreate(sessionId).recentSteps();
    }
}
