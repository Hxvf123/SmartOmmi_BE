package com.smartomni.common.security;

import com.smartomni.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistValidator tokenBlacklistValidator;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Từ chối xác thực khi Token nằm trong Blacklist (sau khi logout)")
    void testDoFilter_TokenIsBlacklisted_RejectsAuthentication() throws Exception {
        String token = "blacklisted.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistValidator.isBlacklisted(token)).thenReturn(true);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistValidator);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(TenantContext.getCurrentUserId());
        assertNull(TenantContext.getTenantId());
    }

    @Test
    @DisplayName("Xác thực thành công và nạp SecurityContext & TenantContext khi Token hợp lệ và không nằm trong Blacklist")
    void testDoFilter_ValidToken_NotBlacklisted_AuthenticatesSuccessfully() throws Exception {
        String token = "valid.jwt.token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenBlacklistValidator.isBlacklisted(token)).thenReturn(false);
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserId(token)).thenReturn(10L);
        when(jwtTokenProvider.getRole(token)).thenReturn("ADMIN");
        when(jwtTokenProvider.getTenantId(token)).thenReturn(100L);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistValidator);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(10L, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals(10L, TenantContext.getCurrentUserId());
        assertEquals("ADMIN", TenantContext.getCurrentRole());
        assertEquals(100L, TenantContext.getTenantId());
    }

    @Test
    @DisplayName("Không xác thực khi Authorization Header không có tiền tố Bearer")
    void testDoFilter_NoBearerHeader_PassesThroughWithoutAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistValidator);
        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(tokenBlacklistValidator, never()).isBlacklisted(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
