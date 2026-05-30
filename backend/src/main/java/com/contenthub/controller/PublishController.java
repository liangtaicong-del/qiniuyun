package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.PublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publish")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Page<PublishTaskResponse>>> getTasks(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long userId = (Long) auth.getPrincipal();
        Page<PublishTaskResponse> tasks = publishService.getTasks(userId, page, size, status);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<PublishTaskResponse>>> publish(
            Authentication auth,
            @RequestBody PublishRequest request) {
        Long userId = (Long) auth.getPrincipal();
        List<PublishTaskResponse> results = publishService.publishArticle(request, userId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PostMapping("/tasks/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTask(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        publishService.cancelTask(id, userId);
        return ResponseEntity.ok(ApiResponse.success("任务已取消", null));
    }

    @PostMapping("/tasks/{id}/retry")
    public ResponseEntity<ApiResponse<PublishTaskResponse>> retryTask(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        PublishTaskResponse result = publishService.retryTask(id, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
