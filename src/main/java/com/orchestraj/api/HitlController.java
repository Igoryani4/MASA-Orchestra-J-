package com.orchestraj.api;

import com.orchestraj.hitl.HitlTaskService;
import com.orchestraj.hitl.HitlTaskView;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/hitl")
public class HitlController {
    private final HitlTaskService hitlTaskService;

    public HitlController(HitlTaskService hitlTaskService) {
        this.hitlTaskService = hitlTaskService;
    }

    @PostMapping("/approve/{taskId}")
    public HitlTaskView approve(@PathVariable String taskId, @RequestBody ApprovalRequest request) {
        try {
            return HitlTaskView.from(hitlTaskService.approve(taskId, request.approved(), request.comment()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @GetMapping("/pending")
    public List<HitlTaskView> pending() {
        return hitlTaskService.listPending().stream()
                .map(HitlTaskView::from)
                .toList();
    }

    @GetMapping("/{taskId}")
    public HitlTaskView task(@PathVariable String taskId) {
        try {
            return HitlTaskView.from(hitlTaskService.get(taskId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    public record ApprovalRequest(boolean approved, @NotBlank String comment) {
    }
}
