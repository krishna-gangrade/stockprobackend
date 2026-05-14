package com.stockpro.eureka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures the Eureka dashboard with HTTP Basic Auth.
 *
 * Credentials are set in application.properties:
 *   spring.security.user.name=eureka
 *   spring.security.user.password=eureka-secret
 *
 * Other services must include these credentials in their eureka.client.serviceUrl:
 *   eureka.client.serviceUrl.defaultZone=http://eureka:eureka-secret@localhost:8761/eureka
 */
@Configuration
@EnableWebSecurity
public class EurekaSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF must be disabled for Eureka peer replication to work correctly
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()  // health check open
                    .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {});   // enable basic auth for dashboard

        return http.build();
    }
}
