package com.smartomni.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Component scan mac dinh se quet ca "com.smartomni.common" (GlobalExceptionHandler,
 * TenantFilter...) vi package goc "com.smartomni.auth" nam duoi "com.smartomni".
 */
@SpringBootApplication(scanBasePackages = "com.smartomni")
@EnableJpaAuditing
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
