package com.contenthub.controller;

import com.contenthub.dto.*;
import com.contenthub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserDTO>> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        AuthResponse.UserDTO profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserDTO>> updateProfile(
            HttpServletRequest request,
            @RequestBody UpdateUserRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        AuthResponse.UserDTO profile = userService.updateProfile(req, userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            HttpServletRequest request,
            @RequestBody ChangePasswordRequest req) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePassword(req, userId);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
    }
}
