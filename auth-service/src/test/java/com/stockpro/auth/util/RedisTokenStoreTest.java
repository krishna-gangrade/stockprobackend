package com.stockpro.auth.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTokenStore unit tests")
class RedisTokenStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtUtil jwtUtil;

    private RedisTokenStore redisTokenStore;

    @BeforeEach
    void setUp() {
        redisTokenStore = new RedisTokenStore(redisTemplate, jwtUtil);
        ReflectionTestUtils.setField(redisTokenStore, "refreshPrefix", "refresh:");
        ReflectionTestUtils.setField(redisTokenStore, "blacklistPrefix", "blacklist:");
        ReflectionTestUtils.setField(redisTokenStore, "sessionPrefix", "session:");

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void storeRefreshToken_persistsTokenWhenValidityIsPositive() {
        when(jwtUtil.getRemainingValidity("refresh-token")).thenReturn(120_000L);

        redisTokenStore.storeRefreshToken(7L, "refresh-token");

        verify(valueOperations).set("refresh:7", "refresh-token", 120_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void storeRefreshToken_skipsExpiredTokens() {
        when(jwtUtil.getRemainingValidity("expired-token")).thenReturn(0L);

        redisTokenStore.storeRefreshToken(7L, "expired-token");

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void getAndDeleteRefreshToken_useExpectedKeys() {
        when(valueOperations.get("refresh:5")).thenReturn("stored-refresh");

        assertThat(redisTokenStore.getRefreshToken(5L)).isEqualTo("stored-refresh");
        redisTokenStore.deleteRefreshToken(5L);

        verify(redisTemplate).delete("refresh:5");
    }

    @Test
    void isRefreshTokenValid_comparesStoredToken() {
        when(valueOperations.get("refresh:3")).thenReturn("token-123");

        assertThat(redisTokenStore.isRefreshTokenValid(3L, "token-123")).isTrue();
        assertThat(redisTokenStore.isRefreshTokenValid(3L, "other-token")).isFalse();
    }

    @Test
    void blacklistAccessToken_persistsTokenWhenRemainingValidityIsPositive() {
        when(jwtUtil.getRemainingValidity("access-token")).thenReturn(30_000L);

        redisTokenStore.blacklistAccessToken("access-token");

        verify(valueOperations).set("blacklist:access-token", "1", 30_000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void isBlacklisted_readsRedisFlag() {
        when(redisTemplate.hasKey("blacklist:abc")).thenReturn(true);

        assertThat(redisTokenStore.isBlacklisted("abc")).isTrue();
    }

    @Test
    void storeAndDeleteSession_useExpectedKeys() {
        redisTokenStore.storeSession("jane@stockpro.com", 11L);
        redisTokenStore.deleteSession("jane@stockpro.com");

        verify(valueOperations).set("session:jane@stockpro.com", "11", 8, TimeUnit.HOURS);
        verify(redisTemplate).delete("session:jane@stockpro.com");
    }
}
