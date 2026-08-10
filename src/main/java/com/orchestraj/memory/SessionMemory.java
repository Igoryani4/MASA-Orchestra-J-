package com.orchestraj.memory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SessionMemory {
    private final Duration ttl;
    private final int maxSteps;
    private final ArrayDeque<Entry> steps = new ArrayDeque<>();

    SessionMemory(Duration ttl, int maxSteps) {
        this.ttl = ttl;
        this.maxSteps = maxSteps;
    }

    void append(String step) {
        cleanup();
        steps.addLast(new Entry(step, Instant.now()));
        while (steps.size() > maxSteps) {
            steps.removeFirst();
        }
    }

    List<String> recentSteps() {
        cleanup();
        return steps.stream().map(Entry::step).toList();
    }

    private void cleanup() {
        Instant border = Instant.now().minus(ttl);
        List<Entry> stale = new ArrayList<>();
        for (Entry entry : steps) {
            if (entry.at().isBefore(border)) {
                stale.add(entry);
            }
        }
        stale.forEach(steps::remove);
    }

    private record Entry(String step, Instant at) {
    }
}
