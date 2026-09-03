package com.godfrey.ai_immigration_document_analyzer.config;

import com.godfrey.ai_immigration_document_analyzer.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Production CORS origins.
     *
     * Example:
     *
     * cors.allowed-origins=https://app.example.com,https://www.example.com
     */
    @Value("${cors.allowed-origins:}")
    private String configuredCorsOrigins;

    /**
     * Password encoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Main Spring Security filter chain.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                /*
                 * =============================================================
                 * CORS
                 * =============================================================
                 */
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                /*
                 * =============================================================
                 * CSRF
                 * =============================================================
                 *
                 * JWT is supplied through the Authorization header.
                 * No session cookie authentication is used.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /*
                 * =============================================================
                 * BASIC AUTH
                 * =============================================================
                 */
                .httpBasic(AbstractHttpConfigurer::disable)

                /*
                 * =============================================================
                 * FORM LOGIN
                 * =============================================================
                 */
                .formLogin(AbstractHttpConfigurer::disable)

                /*
                 * =============================================================
                 * LOGOUT
                 * =============================================================
                 *
                 * JWT authentication is stateless.
                 */
                .logout(AbstractHttpConfigurer::disable)

                /*
                 * =============================================================
                 * SESSION MANAGEMENT
                 * =============================================================
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * =============================================================
                 * AUTHORIZATION
                 * =============================================================
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * -----------------------------------------------------
                         * PUBLIC AUTHENTICATION ENDPOINTS
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify-email",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/status"
                        )
                        .permitAll()

                        /*
                         * -----------------------------------------------------
                         * PUBLIC INFRASTRUCTURE
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        )
                        .permitAll()

                        /*
                         * -----------------------------------------------------
                         * CORS PREFLIGHT
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        /*
                         * -----------------------------------------------------
                         * ADMIN API
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * -----------------------------------------------------
                         * USER DASHBOARD
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                "/api/dashboard/**"
                        )
                        .authenticated()

                        /*
                         * -----------------------------------------------------
                         * DOCUMENTS
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                "/api/documents/**"
                        )
                        .authenticated()

                        /*
                         * -----------------------------------------------------
                         * USER PROFILE
                         * -----------------------------------------------------
                         */
                        .requestMatchers(
                                "/api/users/**",
                                "/api/profile/**"
                        )
                        .authenticated()

                        /*
                         * -----------------------------------------------------
                         * AUTHENTICATED AUTH ENDPOINTS
                         * -----------------------------------------------------
                         *
                         * /me
                         * /change-password
                         * /logout
                         */
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/change-password",
                                "/api/auth/logout"
                        )
                        .authenticated()

                        /*
                         * -----------------------------------------------------
                         * FAIL CLOSED
                         * -----------------------------------------------------
                         */
                        .anyRequest()
                        .authenticated()
                )

                /*
                 * =============================================================
                 * JWT FILTER
                 * =============================================================
                 */
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        log.info(
                "Spring Security filter chain initialized successfully."
        );

        return http.build();
    }

    /**
     * Centralized CORS configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * =============================================================
         * DEFAULT DEVELOPMENT ORIGINS
         * =============================================================
         */
        List<String> allowedOrigins =
                new ArrayList<>(
                        List.of(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173",
                                "http://localhost:4173",
                                "http://127.0.0.1:4173"
                        )
                );

        /*
         * =============================================================
         * PRODUCTION ORIGINS
         * =============================================================
         */
        if (
                configuredCorsOrigins != null
                        &&
                        !configuredCorsOrigins.isBlank()
        ) {

            Arrays.stream(
                            configuredCorsOrigins.split(",")
                    )
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .filter(origin -> !allowedOrigins.contains(origin))
                    .forEach(allowedOrigins::add);
        }

        configuration.setAllowedOrigins(
                List.copyOf(allowedOrigins)
        );

        /*
         * =============================================================
         * HTTP METHODS
         * =============================================================
         */
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * =============================================================
         * REQUEST HEADERS
         * =============================================================
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        /*
         * =============================================================
         * RESPONSE HEADERS
         * =============================================================
         */
        configuration.setExposedHeaders(
                List.of(
                        "Authorization"
                )
        );

        /*
         * =============================================================
         * CREDENTIALS
         * =============================================================
         *
         * JWT is Authorization-header based.
         */
        configuration.setAllowCredentials(false);

        /*
         * =============================================================
         * PREFLIGHT CACHE
         * =============================================================
         */
        configuration.setMaxAge(3600L);

        /*
         * =============================================================
         * REGISTER
         * =============================================================
         */
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        log.debug(
                "CORS configured for origins: {}",
                allowedOrigins
        );

        return source;
    }
}

