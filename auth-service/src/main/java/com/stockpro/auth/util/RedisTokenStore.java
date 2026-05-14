package com.stockpro.auth.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Manages refresh token storage and access token blacklisting in Redis.
 * Session prefix stores userId → refreshToken mapping.
 * Blacklist prefix invalidates logged-out access tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisTokenStore {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Value("${app.redis.refresh-token-prefix}")
    private String refreshPrefix;

    @Value("${app.redis.blacklist-prefix}")
    private String blacklistPrefix;

    @Value("${app.redis.session-prefix}")
    private String sessionPrefix;

    // ── Refresh tokens ────────────────────────────────────────────────────────

    public void storeRefreshToken(Long userId, String refreshToken) {
        String key = refreshPrefix + userId;
        long expiry = jwtUtil.getRemainingValidity(refreshToken);
        if (expiry > 0) {
            redisTemplate.opsForValue().set(key, refreshToken, expiry, TimeUnit.MILLISECONDS);
            log.debug("Stored refresh token for user {}", userId);
        }
    }

    public String getRefreshToken(Long userId) {
        return redisTemplate.opsForValue().get(refreshPrefix + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(refreshPrefix + userId);
    }

    public boolean isRefreshTokenValid(Long userId, String token) {
        String stored = getRefreshToken(userId);
        return token.equals(stored);
    }

    // ── Access token blacklist ─────────────────────────────────────────────────

    public void blacklistAccessToken(String accessToken) {
        long remaining = jwtUtil.getRemainingValidity(accessToken);
        if (remaining > 0) {
            redisTemplate.opsForValue()
                    .set(blacklistPrefix + accessToken, "1", remaining, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistPrefix + accessToken));
    }

    // ── Session (email → userId mapping) ─────────────────────────────────────

    public void storeSession(String email, Long userId) {
        redisTemplate.opsForValue().set(
                sessionPrefix + email,
                userId.toString(),
                8,
                TimeUnit.HOURS
        );
    }

    public void deleteSession(String email) {
        redisTemplate.delete(sessionPrefix + email);
    }
}
