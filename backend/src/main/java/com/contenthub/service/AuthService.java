package com.contenthub.service;

import com.contenthub.common.JwtUtils;
import com.contenthub.dto.*;
import com.contenthub.entity.User;
import com.contenthub.entity.UserSettings;
import com.contenthub.exception.BadRequestException;
import com.contenthub.repository.UserRepository;
import com.contenthub.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("用户名已存在");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("邮箱已被注册");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        UserSettings settings = UserSettings.builder().user(user).build();
        user.setSettings(settings);
        user = userRepository.save(user);

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return buildAuthResponse(token, refreshToken, user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return buildAuthResponse(token, refreshToken, user);
    }

    private AuthResponse buildAuthResponse(String token, String refreshToken, User user) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .bio(user.getBio())
                        .build())
                .build();
    }
}
