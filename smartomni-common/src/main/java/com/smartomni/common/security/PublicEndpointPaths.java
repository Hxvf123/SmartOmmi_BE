package com.smartomni.common.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tiện ích kiểm tra các đường dẫn công khai (/api/v1/auth/**, /api/v1/tenants/onboarding, /error)
 * để đồng bộ quy tắc miễn trừ trên các Filter ngữ cảnh/JWT.
 */
public final class PublicEndpointPaths {

    public static final String[] PUBLIC_AUTH_ONBOARDING_ERROR = {
            "/api/v1/auth/**",
            "/api/v1/tenants/onboarding",
            "/error"
    };

    private static final String AUTH_CHANGE_PASSWORD = "/api/v1/auth/change-password";

    private PublicEndpointPaths() {
    }

    public static boolean isBypassedForTenantFilters(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return isBypassedPath(request.getRequestURI(), true)
                || isBypassedPath(request.getServletPath(), true);
    }

    public static boolean isBypassedForJwtFilter(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return isBypassedPath(request.getRequestURI(), false)
                || isBypassedPath(request.getServletPath(), false);
    }

    private static boolean isBypassedPath(String path, boolean includeChangePassword) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (!includeChangePassword && AUTH_CHANGE_PASSWORD.equals(path)) {
            return false;
        }
        return path.startsWith("/api/v1/auth/")
                || "/api/v1/auth".equals(path)
                || path.startsWith("/api/v1/tenants/onboarding")
                || "/error".equals(path)
                || path.startsWith("/error/");
    }
}

