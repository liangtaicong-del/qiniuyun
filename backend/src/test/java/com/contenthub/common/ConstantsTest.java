package com.contenthub.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    @DisplayName("所有平台应有中文名称")
    void allPlatforms_shouldHaveChineseName() {
        for (Constants.Platform platform : Constants.Platform.values()) {
            assertNotNull(platform.getName());
            assertFalse(platform.getName().isBlank());
        }
    }

    @ParameterizedTest
    @EnumSource(Constants.Platform.class)
    @DisplayName("平台枚举值应可相互转换")
    void platformValueOf_roundTrip(Constants.Platform platform) {
        Constants.Platform restored = Constants.Platform.valueOf(platform.name());
        assertEquals(platform, restored);
    }

    @Test
    @DisplayName("支持8个平台")
    void shouldHaveEightPlatforms() {
        assertEquals(8, Constants.Platform.values().length);
    }

    @Test
    @DisplayName("JWT_PREFIX应为Bearer空格")
    void jwtPrefix_shouldBeBearerSpace() {
        assertEquals("Bearer ", Constants.JWT_PREFIX);
    }
}
