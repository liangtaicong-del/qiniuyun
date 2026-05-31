package com.contenthub.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        setField(jwtUtils, "jwtSecret", "test-secret-key-that-is-at-least-32-characters-long");
        setField(jwtUtils, "jwtExpiration", 86400000L);
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

    @Test
    @DisplayName("应成功生成并解析Token")
    void generateToken_validInput_returnsValidToken() {
        String token = jwtUtils.generateToken(1L, "testuser");

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("应正确从Token中提取用户ID")
    void getUserIdFromToken_validToken_returnsUserId() {
        Long userId = 42L;
        String token = jwtUtils.generateToken(userId, "alice");

        assertEquals(userId, jwtUtils.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("无效Token应返回false")
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
        assertFalse(jwtUtils.validateToken(""));
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    @DisplayName("不同用户ID应生成不同Token")
    void generateToken_differentUsers_returnsDifferentTokens() {
        String token1 = jwtUtils.generateToken(1L, "user1");
        String token2 = jwtUtils.generateToken(2L, "user2");

        assertNotEquals(token1, token2);
    }

    @Nested
    @DisplayName("密钥强度校验")
    class SecretValidation {

        @Test
        @DisplayName("空密钥应在validateSecret时抛出异常")
        void validateSecret_emptySecret_throwsException() {
            JwtUtils utils = new JwtUtils();
            setField(utils, "jwtSecret", "");
            setField(utils, "jwtExpiration", 86400000L);

            assertThrows(IllegalStateException.class, utils::validateSecret);
        }

        @Test
        @DisplayName("短密钥应在validateSecret时抛出异常")
        void validateSecret_shortSecret_throwsException() {
            JwtUtils utils = new JwtUtils();
            setField(utils, "jwtSecret", "too-short");
            setField(utils, "jwtExpiration", 86400000L);

            assertThrows(IllegalStateException.class, utils::validateSecret);
        }
    }
}
