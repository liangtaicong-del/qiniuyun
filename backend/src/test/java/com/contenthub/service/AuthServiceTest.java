package com.contenthub.service;

import com.contenthub.common.JwtUtils;
import com.contenthub.dto.AuthResponse;
import com.contenthub.dto.LoginRequest;
import com.contenthub.dto.RegisterRequest;
import com.contenthub.entity.User;
import com.contenthub.entity.UserSettings;
import com.contenthub.exception.BadRequestException;
import com.contenthub.repository.RefreshTokenRepository;
import com.contenthub.repository.UserRepository;
import com.contenthub.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    private JwtUtils jwtUtils;
    private PasswordEncoder passwordEncoder;
    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jwtUtils = createJwtUtils();
        passwordEncoder = new BCryptPasswordEncoder();

        RefreshTokenRepository mockRefreshRepo = mock(RefreshTokenRepository.class);
        when(mockRefreshRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        refreshTokenService = new RefreshTokenService(mockRefreshRepo, userRepository, jwtUtils);
        setField(refreshTokenService, "refreshExpiration", 604800000L);
        authService = new AuthService(userRepository, userSettingsRepository, jwtUtils, passwordEncoder, refreshTokenService);
    }

    private JwtUtils createJwtUtils() {
        JwtUtils utils = new JwtUtils();
        setField(utils, "jwtSecret", "test-secret-key-that-is-at-least-32-characters-long");
        setField(utils, "jwtExpiration", 86400000L);
        return utils;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("注册测试")
    class RegisterTests {

        @Test
        @DisplayName("新用户注册应成功")
        void register_newUser_success() {
            RegisterRequest req = new RegisterRequest("alice", "alice@example.com", "password123");
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                setField(u, "id", 1L);
                return u;
            });

            AuthResponse resp = authService.register(req);

            assertNotNull(resp);
            assertNotNull(resp.getToken());
            assertEquals("alice", resp.getUser().getUsername());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("重复用户名应抛出异常")
        void register_duplicateUsername_throwsException() {
            RegisterRequest req = new RegisterRequest("existing", "new@example.com", "pass");
            when(userRepository.existsByUsername("existing")).thenReturn(true);

            assertThrows(BadRequestException.class, () -> authService.register(req));
        }

        @Test
        @DisplayName("重复邮箱应抛出异常")
        void register_duplicateEmail_throwsException() {
            RegisterRequest req = new RegisterRequest("newuser", "taken@example.com", "pass");
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThrows(BadRequestException.class, () -> authService.register(req));
        }

        @Test
        @DisplayName("注册时应使用BCrypt加密密码")
        void register_passwordShouldBeEncoded() {
            RegisterRequest req = new RegisterRequest("bob", "bob@example.com", "rawpassword");
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                setField(u, "id", 1L);
                return u;
            });

            authService.register(req);

            String encoded = captor.getValue().getPassword();
            assertNotEquals("rawpassword", encoded);
            assertTrue(passwordEncoder.matches("rawpassword", encoded));
        }
    }

    @Nested
    @DisplayName("登录测试")
    class LoginTests {

        @Test
        @DisplayName("正确凭据应登录成功")
        void login_correctCredentials_success() {
            String rawPassword = "correctPassword";
            User user = User.builder()
                    .id(5L)
                    .username("charlie")
                    .email("charlie@example.com")
                    .password(passwordEncoder.encode(rawPassword))
                    .build();

            when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(user));

            AuthResponse resp = authService.login(new LoginRequest("charlie", rawPassword));

            assertNotNull(resp.getToken());
            assertEquals("charlie", resp.getUser().getUsername());
        }

        @Test
        @DisplayName("错误密码应抛出异常")
        void login_wrongPassword_throwsException() {
            User user = User.builder()
                    .id(1L)
                    .username("dave")
                    .password(passwordEncoder.encode("right"))
                    .build();

            when(userRepository.findByUsername("dave")).thenReturn(Optional.of(user));

            assertThrows(BadRequestException.class,
                    () -> authService.login(new LoginRequest("dave", "wrong")));
        }

        @Test
        @DisplayName("不存在的用户应抛出异常")
        void login_nonexistentUser_throwsException() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> authService.login(new LoginRequest("ghost", "any")));
        }
    }
}
