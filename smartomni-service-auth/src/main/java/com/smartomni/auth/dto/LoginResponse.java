package com.smartomni.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String role;
    private Long tenantId;
    private Long userId;
}
