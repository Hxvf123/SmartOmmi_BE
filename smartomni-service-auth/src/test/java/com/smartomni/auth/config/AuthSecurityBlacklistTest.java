package com.smartomni.auth.config;

import com.smartomni.common.constant.AppConstants;
import com.smartomni.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthSecurityBlacklistTest.TestProtectedAuthController.class)
@Import({SecurityConfig.class, AuthSecurityBlacklistTest.TestProtectedAuthController.class})
class AuthSecurityBlacklistTest {

    @RestController
    static class TestProtectedAuthController {
        @GetMapping("/api/v1/users/profile")
        public String getProfile() {
            return "user-profile-data";
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/auth/login")
        public String login() {
            return "login-ok";
        }

        @org.springframework.web.bind.annotation.PostMapping("/api/v1/tenants/onboarding")
        public String onboarding() {
            return "onboarding-ok";
        }

        @GetMapping("/error")
        public String error() {
            return "error-ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private org.springframework.data.redis.core.ValueOperations<String, String> values;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("Auth service từ chối request khi token nằm trong Redis Blacklist")
    void testAuthServiceRejectsBlacklistedToken() throws Exception {
        String token = "revoked-auth-token";
        when(stringRedisTemplate.hasKey(AppConstants.REDIS_PREFIX_BLACKLIST + token)).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Auth service chấp nhận request khi token hợp lệ và không nằm trong Redis Blacklist")
    void testAuthServiceAcceptsValidNonBlacklistedToken() throws Exception {
        String token = "valid-auth-token";
        when(stringRedisTemplate.hasKey(AppConstants.REDIS_PREFIX_BLACKLIST + token)).thenReturn(false);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn(1L);
        when(jwtTokenProvider.getRole(token)).thenReturn("ADMIN");
        when(jwtTokenProvider.getTenantId(token)).thenReturn(100L);
        when(jwtTokenProvider.getTokenVersion(token)).thenReturn(0L);
        when(stringRedisTemplate.opsForValue()).thenReturn(values);
        when(values.get("auth:user_access:1")).thenReturn("ACTIVE:0");

        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("user-profile-data"));
    }

    @Test
    @DisplayName("Cho phép truy cập công khai không cần token tới /api/v1/auth/login, /api/v1/tenants/onboarding và /error")
    void testPublicRoutesPermittedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("login-ok"));

        mockMvc.perform(post("/api/v1/tenants/onboarding"))
                .andExpect(status().isOk())
                .andExpect(content().string("onboarding-ok"));

        mockMvc.perform(get("/error"))
                .andExpect(status().isOk())
                .andExpect(content().string("error-ok"));
    }
}
