package com.contenthub.service;

import com.contenthub.dto.ChangePasswordRequest;
import com.contenthub.dto.UpdateUserRequest;
import com.contenthub.dto.UserSettingsResponse;
import com.contenthub.dto.AuthResponse;
import com.contenthub.entity.User;
import com.contenthub.exception.BadRequestException;
import com.contenthub.exception.ResourceNotFoundException;
import com.contenthub.repository.UserRepository;
import com.contenthub.service.EmailCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailCodeService emailCodeService;

    public AuthResponse.UserDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return AuthResponse.UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .build();
    }

    @Transactional
    public AuthResponse.UserDTO updateProfile(UpdateUserRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (request.getUsername() != null) {
            userRepository.findByUsername(request.getUsername())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> { throw new BadRequestException("用户名已被使用"); });
            user.setUsername(request.getUsername());
        }
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        user = userRepository.save(user);
        return AuthResponse.UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .build();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("原密码不正确");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("两次输入的密码不一致");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        if (!emailCodeService.verifyCode(email, code)) {
            throw new BadRequestException("验证码无效或已过期");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("该邮箱未注册"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
