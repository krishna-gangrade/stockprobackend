package com.stockpro.auth.util;

import com.stockpro.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User sampleUser;

    // 256-bit secret safe for HS256
    private static final String SECRET =
            "stockpro-test-secret-key-must-be-at-least-256-bits-long-okay";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 28_800_000L, 604_800_000L);

        sampleUser = User.builder()
                .userId(42L)
                .fullName("Test User")
                .email("test@stockpro.com")
                .role(User.Role.INVENTORY_MANAGER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("generates a non-null access token")
    void generateAccessToken_notNull() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generates a non-null refresh token")
    void generateRefreshToken_notNull() {
        String token = jwtUtil.generateRefreshToken(sampleUser);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extracts correct email from access token")
    void extractEmail_correct() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("test@stockpro.com");
    }

    @Test
    @DisplayName("extracts correct userId from access token")
    void extractUserId_correct() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("extracts correct role from access token")
    void extractRole_correct() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("INVENTORY_MANAGER");
    }

    @Test
    @DisplayName("isTokenValid returns true for fresh token")
    void isTokenValid_freshToken_true() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for garbage string")
    void isTokenValid_garbage_false() {
        assertThat(jwtUtil.isTokenValid("not.a.real.jwt")).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for empty string")
    void isTokenValid_empty_false() {
        assertThat(jwtUtil.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("isRefreshToken returns true for refresh token")
    void isRefreshToken_refreshToken_true() {
        String token = jwtUtil.generateRefreshToken(sampleUser);
        assertThat(jwtUtil.isRefreshToken(token)).isTrue();
    }

    @Test
    @DisplayName("isRefreshToken returns false for access token")
    void isRefreshToken_accessToken_false() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(jwtUtil.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("getRemainingValidity returns positive ms for fresh token")
    void getRemainingValidity_positive() {
        String token = jwtUtil.generateAccessToken(sampleUser);
        assertThat(jwtUtil.getRemainingValidity(token)).isPositive();
    }

    @Test
    @DisplayName("access and refresh tokens are different strings")
    void accessAndRefreshTokens_areDifferent() {
        String access = jwtUtil.generateAccessToken(sampleUser);
        String refresh = jwtUtil.generateRefreshToken(sampleUser);
        assertThat(access).isNotEqualTo(refresh);
    }

    @Test
    @DisplayName("tokens for two different users are different")
    void tokensForDifferentUsers_areDifferent() {
        User user2 = User.builder()
                .userId(99L).email("other@stockpro.com")
                .fullName("Other User")
                .role(User.Role.WAREHOUSE_STAFF).isActive(true)
                .createdAt(LocalDateTime.now()).version(0L).build();

        String t1 = jwtUtil.generateAccessToken(sampleUser);
        String t2 = jwtUtil.generateAccessToken(user2);
        assertThat(t1).isNotEqualTo(t2);
    }
}
