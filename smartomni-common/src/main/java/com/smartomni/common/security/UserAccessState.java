package com.smartomni.common.security;

public final class UserAccessState {
    public static final String PREFIX = "auth:user_access:";

    private UserAccessState() { }

    public static String key(Long userId) { return PREFIX + userId; }
    public static String active(Long version) { return "ACTIVE:" + version; }
    public static String disabled(Long version) { return "DISABLED:" + version; }
}
