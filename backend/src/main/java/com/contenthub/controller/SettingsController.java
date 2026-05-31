package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserSettingsResponse settings = settingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            HttpServletRequest request,
            @RequestBody UserSettingsRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        UserSettingsResponse settings = settingsService.updateSettings(req, userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
}
