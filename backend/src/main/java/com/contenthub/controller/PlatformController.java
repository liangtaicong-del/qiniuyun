package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<PlatformAccountResponse>>> getUserPlatforms(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<PlatformAccountResponse> platforms = platformService.getUserPlatforms(userId);
        return ResponseEntity.ok(ApiResponse.success(platforms));
    }

    @PostMapping("/bind")
    public ResponseEntity<ApiResponse<PlatformAccountResponse>> bindPlatform(
            Authentication auth,
            @RequestBody BindPlatformRequest request) {
        Long userId = (Long) auth.getPrincipal();
        PlatformAccountResponse response = platformService.bindPlatform(request.getPlatform(), userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{platform}/unbind")
    public ResponseEntity<ApiResponse<Void>> unbindPlatform(Authentication auth, @PathVariable String platform) {
        Long userId = (Long) auth.getPrincipal();
        platformService.unbindPlatform(platform, userId);
        return ResponseEntity.ok(ApiResponse.success("解绑成功", null));
    }
}
