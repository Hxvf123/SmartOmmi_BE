package com.smartomni.common.exception;

import com.smartomni.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void genericFailureDoesNotExposeSql() {
        var response = handler.handleGenericException(
                new RuntimeException("INSERT INTO tenants (id) VALUES (42) failed"));
        ApiResponse<Object> body = response.getBody();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.getCode());
        assertFalse(body.getMessage().contains("INSERT"));
        assertFalse(body.getMessage().contains("tenants"));
    }

    @Test
    void businessFailureKeepsStableCode() {
        var response = handler.handleBusinessException(
                new BusinessException("Subdomain da ton tai", HttpStatus.CONFLICT, "SUBDOMAIN_ALREADY_EXISTS"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("SUBDOMAIN_ALREADY_EXISTS", response.getBody().getCode());
    }
}
