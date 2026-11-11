package com.smartomni.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenApiSecurityAutoConfigurationTest {

    @Test
    void swaggerUsesTheGatewayOrigin() {
        var openApi = new OpenApiSecurityAutoConfiguration().smartomniOpenApi();

        assertEquals("/", openApi.getServers().get(0).getUrl());
        assertNotNull(openApi.getComponents().getSecuritySchemes().get("bearerAuth"));
    }
}
