package com.smartomni.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InternalJobTokenFilterTest {
    private final InternalJobTokenFilter filter = new InternalJobTokenFilter("0123456789abcdef0123456789abcdef");

    @Test
    void rejectsInternalJobWithoutToken() throws Exception {
        var request = new MockHttpServletRequest("POST", "/internal/jobs/poll-orders");
        request.setServletPath("/internal/jobs/poll-orders");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void acceptsMatchingToken() throws Exception {
        var request = new MockHttpServletRequest("POST", "/internal/jobs/poll-orders");
        request.setServletPath("/internal/jobs/poll-orders");
        request.addHeader("X-Internal-Job-Token", "0123456789abcdef0123456789abcdef");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }
}
