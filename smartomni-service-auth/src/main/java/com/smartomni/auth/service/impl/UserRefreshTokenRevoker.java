package com.smartomni.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRefreshTokenRevoker {
    private final StringRedisTemplate redis;

    public void revoke(Long userId) {
        String userKey = AuthServiceImpl.REDIS_PREFIX_USER_REFRESH + userId;
        String token = redis.opsForValue().get(userKey);
        if (token != null) {
            redis.delete(AuthServiceImpl.REDIS_PREFIX_REFRESH + token);
        }
        redis.delete(userKey);
    }
}
