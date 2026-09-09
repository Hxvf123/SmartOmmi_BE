package com.smartomni.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/** UC-17: Dang ky & khoi tao Tenant (Self-service Onboarding). */
@Getter
@Setter
public class TenantOnboardingRequest {

    @NotBlank
    private String businessName;

    @NotBlank
    @Pattern(regexp = "^[a-z0-9-]{3,30}$", message = "Subdomain chi gom chu thuong, so va dau gach ngang, 3-30 ky tu")
    private String subdomain;

    @NotBlank
    private String adminEmail;

    @NotBlank
    private String adminPassword;

    private String initialPlanName = "Free";
}
