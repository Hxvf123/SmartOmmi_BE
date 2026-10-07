package com.smartomni.migrations;

import com.smartomni.auth.service.AuthService;
import com.smartomni.common.persistence.RlsDatabaseConfig;
import com.smartomni.common.security.JwtTokenProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Boot's real Flyway/JPA startup path with entities from all seven services. */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = {
        "com.smartomni.auth.entity", "com.smartomni.tenant.entity", "com.smartomni.catalog.entity",
        "com.smartomni.inventory.entity", "com.smartomni.order.entity",
        "com.smartomni.integration.entity", "com.smartomni.ai.entity"
})
@EnableJpaRepositories(basePackages = "com.smartomni.auth.repository")
@Import({RlsDatabaseConfig.class, AuthService.class, JwtTokenProvider.class})
public class MigrationTestApplication {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
