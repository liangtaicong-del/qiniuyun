package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getSettings(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        UserSettingsResponse settings = settingsService.getSettings(userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            Authentication auth,
            @RequestBody UserSettingsRequest request) {
        Long userId = (Long) auth.getPrincipal();
        UserSettingsResponse settings = settingsService.updateSettings(request, userId);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
}
