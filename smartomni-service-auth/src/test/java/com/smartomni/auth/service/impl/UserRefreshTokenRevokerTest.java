package com.smartomni.auth.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

class UserRefreshTokenRevokerTest {
    @Test void revokesOnlyExactUserAndReverseTokenKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:user_refresh:1")).thenReturn("token-1");

        new UserRefreshTokenRevoker(redis).revoke(1L);

        verify(redis).delete("auth:refresh_token:token-1");
        verify(redis).delete("auth:user_refresh:1");
        verify(redis, never()).keys(anyString());
        verify(redis, never()).delete("auth:user_refresh:10");
    }

    @Test void missingTokenStillDeletesExactUserKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        new UserRefreshTokenRevoker(redis).revoke(1L);

        verify(redis).delete("auth:user_refresh:1");
        verify(redis, times(1)).delete(anyString());
    }
}
