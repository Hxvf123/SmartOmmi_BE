package com.smartomni.auth.controller;

import com.smartomni.auth.dto.ForgotPasswordRequest;
import com.smartomni.auth.dto.LoginRequest;
import com.smartomni.auth.dto.LoginResponse;
import com.smartomni.auth.dto.ResetPasswordRequest;
import com.smartomni.auth.service.AuthService;
import com.smartomni.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * UC-01: Dang nhap he thong
 * UC-02: Dang xuat he thong (stateless JWT - client tu xoa token; endpoint /logout
 *         danh cho truong hop can them token vao blacklist/Redis trong tuong lai)
 * UC-03: Quen mat khau / Dat lai mat khau
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // TODO: neu can revoke ngay lap tuc, luu jti vao Redis blacklist tai day (UC-02 / FR-004)
        return ApiResponse.success("Dang xuat thanh cong", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ApiResponse.success("Neu email ton tai, huong dan dat lai mat khau da duoc gui", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.success("Dat lai mat khau thanh cong", null);
    }
}
