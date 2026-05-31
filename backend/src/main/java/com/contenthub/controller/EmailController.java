package com.contenthub.controller;

import com.contenthub.dto.ApiResponse;
import com.contenthub.service.EmailCodeService;
import com.contenthub.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailController {

    private final EmailCodeService emailCodeService;
    private final UserService userService;

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String type = body.getOrDefault("type", "register");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("邮箱不能为空"));
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("邮箱格式不正确"));
        }

        emailCodeService.sendCode(email, type);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetRequest req) {
        userService.resetPassword(req.email, req.code, req.newPassword);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }

    @Data
    public static class ResetRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "密码长度至少6位")
        private String newPassword;
    }
}
