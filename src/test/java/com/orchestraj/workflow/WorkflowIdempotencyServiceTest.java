package com.orchestraj.workflow;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkflowIdempotencyServiceTest {

    @Test
    void shouldExecuteSyncWorkflowOnlyOncePerKey() {
        var service = new WorkflowIdempotencyService();
        var request = new WorkflowRequest("s1", "g1", "objective", "biomed", "{\"task\":\"x\"}");
        AtomicInteger launches = new AtomicInteger(0);

        Mono<WorkflowStatus> first = service.runSync("k1", request, () -> {
            launches.incrementAndGet();
            return Mono.just(new WorkflowStatus("s1", "g1", "COMPLETED", "ok", "{}"));
        });
        Mono<WorkflowStatus> second = service.runSync("k1", request, () -> {
            launches.incrementAndGet();
            return Mono.just(new WorkflowStatus("s1", "g1", "COMPLETED", "ok", "{}"));
        });

        first.block();
        second.block();
        assertEquals(1, launches.get());
    }

    @Test
    void shouldReuseAsyncTaskIdPerKey() {
        var service = new WorkflowIdempotencyService();
        var request = new WorkflowRequest("s1", "g1", "objective", "biomed", "{\"task\":\"x\"}");
        AtomicInteger launches = new AtomicInteger(0);

        String firstTaskId = service.resolveAsyncTaskId("k1", request, () -> {
            launches.incrementAndGet();
            return "task-1";
        });
        String secondTaskId = service.resolveAsyncTaskId("k1", request, () -> {
            launches.incrementAndGet();
            return "task-2";
        });

        assertEquals("task-1", firstTaskId);
        assertEquals("task-1", secondTaskId);
        assertEquals(1, launches.get());
    }

    @Test
    void shouldRejectDifferentRequestsForSameKey() {
        var service = new WorkflowIdempotencyService();
        var firstRequest = new WorkflowRequest("s1", "g1", "objective", "biomed", "{\"task\":\"x\"}");
        var secondRequest = new WorkflowRequest("s1", "g1", "another", "biomed", "{\"task\":\"x\"}");

        service.resolveAsyncTaskId("k1", firstRequest, () -> "task-1");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.resolveAsyncTaskId("k1", secondRequest, () -> "task-2")
        );
    }
}
