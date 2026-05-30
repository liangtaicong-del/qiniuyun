package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserDTO>> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        AuthResponse.UserDTO profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserDTO>> updateProfile(
            Authentication auth,
            @RequestBody UpdateUserRequest request) {
        Long userId = (Long) auth.getPrincipal();
        AuthResponse.UserDTO profile = userService.updateProfile(request, userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @RequestBody ChangePasswordRequest request) {
        Long userId = (Long) auth.getPrincipal();
        userService.changePassword(request, userId);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
    }
}
