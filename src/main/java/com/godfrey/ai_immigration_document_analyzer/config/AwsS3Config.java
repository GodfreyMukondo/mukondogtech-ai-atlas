package com.godfrey.ai_immigration_document_analyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * ============================================================================
 * AWS S3 CONFIGURATION
 * ============================================================================
 *
 * Provides the application's singleton S3Client.
 *
 * AWS SDK DefaultCredentialsProvider automatically resolves credentials from
 * supported sources such as:
 *
 * 1. Environment variables
 * 2. AWS profile configuration
 * 3. ECS task credentials
 * 4. EC2 instance roles
 * 5. Other AWS credential providers
 *
 * This avoids hard-coding AWS credentials in source code.
 */
@Configuration
@Slf4j
public class AwsS3Config {

    @Value("${aws.region:eu-north-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {

        Region region = Region.of(
                awsRegion.trim()
        );

        log.info(
                "Initializing AWS S3 client | region={} | bucket={}",
                region.id(),
                bucketName
        );

        return S3Client.builder()
                .region(region)
                .credentialsProvider(
                        DefaultCredentialsProvider.create()
                )
                .build();
    }
}