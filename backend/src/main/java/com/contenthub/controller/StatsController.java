package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StatsResponse>> getOverview(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        StatsResponse stats = statsService.getOverview(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/platforms")
    public ResponseEntity<ApiResponse<List<StatsResponse.PlatformStats>>> getPlatformStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        StatsResponse stats = statsService.getOverview(userId);
        return ResponseEntity.ok(ApiResponse.success(stats.getPlatforms()));
    }
}
