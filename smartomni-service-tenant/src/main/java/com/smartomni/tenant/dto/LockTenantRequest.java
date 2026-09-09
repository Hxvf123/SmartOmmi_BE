package com.smartomni.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** UC-19: Khoa/mo tai khoan Tenant. */
@Getter
@Setter
public class LockTenantRequest {

    @NotBlank
    private String reason;
}
