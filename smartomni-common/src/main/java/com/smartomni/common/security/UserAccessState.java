package com.smartomni.common.security;

public final class UserAccessState {
    public static final String PREFIX = "auth:user_access:";

    private UserAccessState() { }

    public static String key(Long userId) { return PREFIX + userId; }
    public static String active(Long version) { return "ACTIVE:" + version; }
    public static String disabled(Long version) { return "DISABLED:" + version; }

    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> PUBLISH_ACTIVE =
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                    "local current = redis.call('GET', KEYS[1]); " +
                    "if current then " +
                    "  local sep = string.find(current, ':'); " +
                    "  local oldVersion = tonumber(string.sub(current, sep + 1)); " +
                    "  local newVersion = tonumber(ARGV[1]); " +
                    "  if oldVersion > newVersion or (oldVersion == newVersion and current ~= 'ACTIVE:' .. ARGV[1]) then return 0 end; " +
                    "end; " +
                    "redis.call('SET', KEYS[1], 'ACTIVE:' .. ARGV[1]); return 1",
                    Long.class);

    public static boolean publishActive(org.springframework.data.redis.core.StringRedisTemplate redis,
                                        Long userId, Long version) {
        return Long.valueOf(1L).equals(redis.execute(PUBLISH_ACTIVE,
                java.util.List.of(key(userId)), String.valueOf(version)));
    }
}
