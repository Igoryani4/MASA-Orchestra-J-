package com.orchestraj.api;

import com.orchestraj.hitl.HitlTaskService;
import com.orchestraj.hitl.PendingTask;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hitl")
public class HitlController {
    private final HitlTaskService hitlTaskService;

    public HitlController(HitlTaskService hitlTaskService) {
        this.hitlTaskService = hitlTaskService;
    }

    @PostMapping("/approve/{taskId}")
    public PendingTask approve(@PathVariable String taskId, @RequestBody ApprovalRequest request) {
        return hitlTaskService.approve(taskId, request.approved(), request.comment());
    }

    public record ApprovalRequest(boolean approved, @NotBlank String comment) {
    }
}
