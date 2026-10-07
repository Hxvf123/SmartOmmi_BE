package com.smartomni.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Luu y quan trong: viec HUAN LUYEN mo hinh AI (Prophet/ARIMA, Collaborative
 * Filtering) duoc thuc hien boi mot AI microservice rieng VIET BANG PYTHON
 * (nam ngoai repo Spring Boot nay). Service Java nay CHI dong vai tro:
 *   1) Goi REST sang AI Service (Python) de yeu cau training/prediction
 *   2) Luu ket qua (forecast, recommendation, model version) vao Postgres
 *   3) Expose API cho cac service khac / Admin ERP doc ket qua
 */
@SpringBootApplication(scanBasePackages = "com.smartomni")
@EnableJpaAuditing
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
