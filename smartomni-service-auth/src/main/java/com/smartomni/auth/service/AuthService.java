package com.smartomni.auth.service;

import com.smartomni.auth.dto.LoginRequest;
import com.smartomni.auth.dto.LoginResponse;
import com.smartomni.auth.entity.PasswordResetToken;
import com.smartomni.auth.entity.User;
import com.smartomni.auth.repository.PasswordResetTokenRepository;
import com.smartomni.auth.repository.UserRepository;
import com.smartomni.common.exception.BusinessException;
import com.smartomni.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Trien khai UC-01 (Dang nhap), UC-02 (Dang xuat - xu ly stateless o phia
 * client/JwtAuthenticationFilter, o day chi minh hoa revoke option),
 * UC-03 (Quen mat khau / Dat lai mat khau).
 * FR-001 -> FR-007.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    // private final MailService mailService; // TODO: tich hop gui email that (UC-03)

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Email hoac mat khau khong dung", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS"));

        // FR-007: chan brute-force
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException("Tai khoan tam khoa do dang nhap sai qua nhieu lan. Vui long thu lai sau.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_TEMP_LOCKED");
        }

        if (user.getStatus() == User.UserStatus.DISABLED) {
            throw new BusinessException("Tai khoan da bi vo hieu hoa", HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BusinessException("Email hoac mat khau khong dung", HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }

        // TODO: FR-003 - goi Tenant Service kiem tra tenant.status != LOCKED truoc khi cap JWT
        // (bo qua neu user.role == SUPER_ADMIN vi khong gan tenant)

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getRole().name(), user.getTenantId());
        return new LoginResponse(token, user.getRole().name(), user.getTenantId(), user.getId());
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() == null ? 1 : user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
        }
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        // FR-005: khong tiet lo email co ton tai hay khong (bao mat)
        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getId());
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
            resetTokenRepository.save(resetToken);

            // TODO: mailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new BusinessException("Token khong hop le hoac da duoc su dung"));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token dat lai mat khau da het han");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException("Nguoi dung khong ton tai"));

        user.setPasswordHash(passwordEncoder.encode(newPassword)); // FR-006: luon hash truoc khi luu
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }
}
