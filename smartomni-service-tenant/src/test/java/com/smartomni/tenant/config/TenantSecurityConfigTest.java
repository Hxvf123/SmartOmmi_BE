package com.smartomni.tenant.config;

import com.smartomni.common.security.JwtTokenProvider;
import com.smartomni.common.security.TokenBlacklistValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TenantSecurityConfigTest.TestTenantSecurityController.class)
@Import({SecurityConfig.class, TenantSecurityConfigTest.TestTenantSecurityController.class})
class TenantSecurityConfigTest {

    @RestController
    static class TestTenantSecurityController {
        @PostMapping("/api/v1/auth/login")
        public String login() {
            return "login-ok";
        }

        @PostMapping("/api/v1/tenants/onboarding")
        public String onboarding() {
            return "onboarding-ok";
        }

        @GetMapping("/error")
        public String error() {
            return "error-ok";
        }

        @GetMapping("/api/v1/tenants/me")
        public String me() {
            return "me";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TokenBlacklistValidator tokenBlacklistValidator;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("Cho phép truy cập công khai không cần Token vào /api/v1/auth/**, /api/v1/tenants/onboarding và /error")
    void testPublicEndpointsAllowedWithoutToken() throws Exception {
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

    @Test
    @DisplayName("Endpoint bảo mật /api/v1/tenants/me vẫn từ chối khi không có Token")
    void testProtectedEndpointRejectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/me"))
                .andExpect(status().isForbidden());
    }
}

