package com.stockpro.auth.config;

import com.stockpro.auth.entity.User;
import com.stockpro.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap-admin.full-name:StockPro Admin}")
    private String bootstrapAdminName;

    @Value("${app.bootstrap-admin.email:admin@stockpro.local}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String bootstrapAdminPassword;

    @Bean
    public ApplicationRunner bootstrapAdminRunner() {
        return args -> {
            if (!bootstrapEnabled || userRepository.existsByRole(User.Role.ADMIN)) {
                return;
            }

            if (bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank() || bootstrapAdminPassword.length() < 8) {
                log.warn("Bootstrap admin skipped because password is missing or too short");
                return;
            }

            User admin = User.builder()
                    .fullName(bootstrapAdminName)
                    .email(bootstrapAdminEmail.toLowerCase())
                    .passwordHash(passwordEncoder.encode(bootstrapAdminPassword))
                    .role(User.Role.ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
            log.warn("Bootstrap admin created with email {}", bootstrapAdminEmail);
        };
    }
}
