package com.contenthub.service;

import com.contenthub.common.JwtUtils;
import com.contenthub.dto.AuthResponse;
import com.contenthub.entity.RefreshToken;
import com.contenthub.entity.User;
import com.contenthub.exception.BadRequestException;
import com.contenthub.repository.RefreshTokenRepository;
import com.contenthub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Transactional
    public String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusNanos(refreshExpiration * 1_000_000L))
                .build();
        token = refreshTokenRepository.save(token);
        return token.getToken();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BadRequestException("Refresh token not found"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new BadRequestException("Refresh token has expired");
        }

        User user = stored.getUser();
        String newAccessToken = jwtUtils.generateToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .token(newAccessToken)
                .user(AuthResponse.UserDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .bio(user.getBio())
                        .build())
                .build();
    }

    @Transactional
    public void revokeUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("已吊销用户 {} 的所有Refresh Token", userId);
    }
}
