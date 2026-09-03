package com.godfrey.ai_immigration_document_analyzer.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;



/**
 * ============================================================
 * APPLICATION CONFIGURATION
 * ============================================================
 *
 * Global application beans.
 *
 * Responsibilities:
 *
 * - Register shared application utilities
 * - Configure reusable Spring beans
 *
 * Note:
 *
 * ObjectMapper is managed by Spring Boot automatically.
 * Custom Jackson configuration should be placed in
 * JacksonConfig.java instead.
 *
 * ============================================================
 */
@Configuration
public class AppConfig {





    /**
     * ============================================================
     * REST TEMPLATE
     * ============================================================
     *
     * Used for:
     *
     * - External API calls
     * - Third-party integrations
     * - AI service communication
     *
     * ============================================================
     */
    @Bean
    public RestTemplate restTemplate() {


        return new RestTemplate();


    }



}