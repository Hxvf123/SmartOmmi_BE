package com.smartomni.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Nem ra khi Tenant co goi dich vu khong bao gom tinh nang duoc yeu cau,
 * hoac vuot gioi han tai nguyen cua goi (UC-41 / FR-077, FR-078).
 */
public class FeatureNotAvailableException extends BusinessException {
    public FeatureNotAvailableException(String message) {
        super(message, HttpStatus.FORBIDDEN, "FEATURE_NOT_AVAILABLE");
    }
}
