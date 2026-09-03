package com.godfrey.ai_immigration_document_analyzer.security;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}.
 *
 * These are plain unit tests (no Spring context): the @Value-injected
 * fields are set directly via ReflectionTestUtils so token generation,
 * validation, expiration and tampering behaviour can be verified in
 * isolation.
 */
class JwtServiceTest {

    private static final String VALID_SECRET =
            "this-is-a-test-secret-key-that-is-at-least-32-bytes-long";

    private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", VALID_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3_600_000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "ai-immigration-document-analyzer");

        user = User.builder()
                .id(42L)
                .fullName("Jane Doe")
                .email("Jane.Doe@Example.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Test
    void generatesAndValidatesRoundTripToken() {

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.isTokenExpired(token)).isFalse();

        assertThat(jwtService.extractUsername(token)).isEqualTo("jane.doe@example.com");
        assertThat(jwtService.extractEmail(token)).isEqualTo("jane.doe@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void adminRoleIsPreservedInToken() {

        user.setRole(Role.ADMIN);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void expiredTokenIsInvalid() {

        ReflectionTestUtils.setField(jwtService, "expirationTime", -1_000L);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isFalse();
        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {

        String token = jwtService.generateToken(user);

        JwtService attackerService = new JwtService();

        ReflectionTestUtils.setField(
                attackerService,
                "secretKey",
                "a-completely-different-secret-key-of-32-plus-bytes"
        );
        ReflectionTestUtils.setField(attackerService, "expirationTime", 3_600_000L);
        ReflectionTestUtils.setField(attackerService, "issuer", "ai-immigration-document-analyzer");

        assertThat(attackerService.isTokenValid(token)).isFalse();
    }

    @Test
    void tokenWithWrongIssuerIsRejected() {

        String token = jwtService.generateToken(user);

        JwtService otherIssuerService = new JwtService();

        ReflectionTestUtils.setField(otherIssuerService, "secretKey", VALID_SECRET);
        ReflectionTestUtils.setField(otherIssuerService, "expirationTime", 3_600_000L);
        ReflectionTestUtils.setField(otherIssuerService, "issuer", "some-other-service");

        assertThat(otherIssuerService.isTokenValid(token)).isFalse();
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {

        ReflectionTestUtils.setField(jwtService, "secretKey", "too-short");

        assertThatThrownBy(() -> jwtService.generateToken(user))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingSecret() {

        ReflectionTestUtils.setField(jwtService, "secretKey", "");

        assertThatThrownBy(() -> jwtService.generateToken(user))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNullUser() {

        assertThatThrownBy(() -> jwtService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUserWithoutId() {

        user.setId(null);

        assertThatThrownBy(() -> jwtService.generateToken(user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUserWithBlankEmail() {

        user.setEmail("  ");

        assertThatThrownBy(() -> jwtService.generateToken(user))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankTokenIsInvalidNotAnException() {

        assertThat(jwtService.isTokenValid("")).isFalse();
        assertThat(jwtService.isTokenValid(null)).isFalse();
    }
}
