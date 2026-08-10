package com.orchestraj.hitl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HitlTaskServiceTest {

    @Test
    void shouldListOnlyPendingTasks() {
        var service = new HitlTaskService();
        PendingTask pending = service.create("s1", "need review");
        PendingTask approved = service.create("s2", "need review");
        service.approve(approved.taskId(), true, "ok");

        var pendingTasks = service.listPending();
        assertEquals(1, pendingTasks.size());
        assertEquals(pending.taskId(), pendingTasks.get(0).taskId());
    }
}
