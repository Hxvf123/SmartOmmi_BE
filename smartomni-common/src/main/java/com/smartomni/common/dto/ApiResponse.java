package com.smartomni.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Wrapper chuan cho moi response API trong toan he thong.
 * Khong bao gio expose JPA entity truc tiep qua Controller - luon di qua DTO nay.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String code;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, null, data, Instant.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, null, data, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, message, code, null, Instant.now());
    }
}
