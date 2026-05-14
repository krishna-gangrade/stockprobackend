package com.stockpro.admin;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * Secures the Spring Boot Admin dashboard UI.
 *
 * - Login page available at /login
 * - Admin dashboard at /  (requires login)
 * - Actuator endpoints open (so monitored services can POST their status)
 * - Static assets open (JS, CSS needed for the UI to load)
 *
 * Credentials are set in application.properties:
 *   spring.security.user.name=admin
 *   spring.security.user.password=admin-secret
 */
@Configuration
@EnableWebSecurity
public class AdminSecurityConfig {

    private final AdminServerProperties adminServerProperties;

    public AdminSecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminServerProperties = adminServerProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Success handler redirects to Admin dashboard after login
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminServerProperties.path("/"));

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    // Open: static assets, login page, actuator (for service health posts)
                    .requestMatchers(adminServerProperties.path("/assets/**")).permitAll()
                    .requestMatchers(adminServerProperties.path("/login")).permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    // Everything else requires login
                    .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .loginPage(adminServerProperties.path("/login"))
                    .successHandler(successHandler)
            )
            .logout(logout -> logout
                    .logoutUrl(adminServerProperties.path("/logout"))
            )
            .httpBasic(httpBasic -> {}); // allow basic auth for programmatic access

        return http.build();
    }
}
