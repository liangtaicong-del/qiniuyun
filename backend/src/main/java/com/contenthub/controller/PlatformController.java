package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<PlatformAccountResponse>>> getUserPlatforms(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<PlatformAccountResponse> platforms = platformService.getUserPlatforms(userId);
        return ResponseEntity.ok(ApiResponse.success(platforms));
    }

    @PostMapping("/bind")
    public ResponseEntity<ApiResponse<PlatformAccountResponse>> bindPlatform(
            HttpServletRequest request,
            @RequestBody BindPlatformRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        PlatformAccountResponse response = platformService.bindPlatform(req.getPlatform(), userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{platform}/unbind")
    public ResponseEntity<ApiResponse<Void>> unbindPlatform(HttpServletRequest request, @PathVariable String platform) {
        Long userId = (Long) request.getAttribute("userId");
        platformService.unbindPlatform(platform, userId);
        return ResponseEntity.ok(ApiResponse.success("解绑成功", null));
    }
}
