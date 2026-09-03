package com.godfrey.ai_immigration_document_analyzer.security;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secretKey;

    @Value("${security.jwt.expiration:3600000}")
    private long expirationTime;

    @Value("${spring.application.name:ai-immigration-document-analyzer}")
    private String issuer;

    /**
     * Creates the HMAC signing key.
     */
    private SecretKey getSigningKey() {

        if (
                secretKey == null
                        ||
                        secretKey.isBlank()
        ) {
            throw new IllegalStateException(
                    "JWT secret key is not configured."
            );
        }

        /*
         * Minimum length for an HS256-compatible UTF-8 secret.
         *
         * Production secrets should be considerably stronger than
         * this minimum and generated randomly.
         */
        if (secretKey.getBytes(StandardCharsets.UTF_8).length < 32) {

            throw new IllegalStateException(
                    "JWT secret must contain at least 32 bytes."
            );
        }

        return Keys.hmacShaKeyFor(
                secretKey.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    /**
     * Generates an access token for a user.
     */
    public String generateToken(
            User user
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Cannot generate JWT for null user."
            );
        }

        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "Cannot generate JWT for a user without an ID."
            );
        }

        if (
                user.getEmail() == null
                        ||
                        user.getEmail().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Cannot generate JWT for a user without an email."
            );
        }

        Date issuedAt =
                new Date();

        Date expiresAt =
                new Date(
                        issuedAt.getTime()
                                +
                                expirationTime
                );

        String role =
                normalizeRole(
                        user.getRole()
                );

        return Jwts.builder()
                .issuer(issuer)
                .subject(
                        user.getEmail()
                                .trim()
                                .toLowerCase()
                )
                .claim(
                        "userId",
                        user.getId()
                )
                .claim(
                        "role",
                        role
                )
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the authenticated username/email.
     */
    public String extractUsername(
            String token
    ) {
        return extractClaims(token)
                .getSubject();
    }

    /**
     * Email alias.
     */
    public String extractEmail(
            String token
    ) {
        return extractUsername(token);
    }

    /**
     * Extracts user ID.
     */
    public Long extractUserId(
            String token
    ) {

        Object userId =
                extractClaims(token)
                        .get("userId");

        if (userId == null) {
            return null;
        }

        try {

            return Long.parseLong(
                    userId.toString()
            );

        } catch (NumberFormatException exception) {

            log.debug(
                    "JWT contains an invalid userId claim."
            );

            return null;
        }
    }

    /**
     * Extracts role claim.
     */
    public String extractRole(
            String token
    ) {

        return extractClaims(token)
                .get(
                        "role",
                        String.class
                );
    }

    /**
     * Parses and verifies JWT claims.
     */
    private Claims extractClaims(
            String token
    ) {

        if (
                token == null
                        ||
                        token.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "JWT token cannot be empty."
            );
        }

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates signature, issuer and expiration.
     */
    public boolean isTokenValid(
            String token
    ) {

        try {

            Claims claims =
                    extractClaims(token);

            Date expiration =
                    claims.getExpiration();

            if (expiration == null) {
                return false;
            }

            return expiration.after(
                    new Date()
            );

        } catch (
                JwtException
                |
                IllegalArgumentException exception
        ) {

            log.debug(
                    "JWT validation failed: {}",
                    exception.getMessage()
            );

            return false;
        }
    }

    /**
     * Checks whether a token is expired.
     */
    public boolean isTokenExpired(
            String token
    ) {

        try {

            Date expiration =
                    extractClaims(token)
                            .getExpiration();

            return expiration == null
                    ||
                    expiration.before(
                            new Date()
                    );

        } catch (Exception exception) {

            return true;
        }
    }

    /**
     * Converts Role enum to JWT role value.
     */
    private String normalizeRole(
            Role role
    ) {

        if (role == null) {
            return Role.USER.name();
        }

        return role.name()
                .replace(
                        "ROLE_",
                        ""
                )
                .toUpperCase();
    }
}

