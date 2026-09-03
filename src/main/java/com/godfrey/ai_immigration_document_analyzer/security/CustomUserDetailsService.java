package com.godfrey.ai_immigration_document_analyzer.security;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * ============================================================================
 * CUSTOM USER DETAILS SERVICE
 * ============================================================================
 *
 * Loads authenticated application users from the database.
 *
 * Security responsibilities:
 *
 * - Normalize email addresses.
 * - Load the user from the database.
 * - Validate that a security role exists.
 * - Convert the application role into a Spring Security authority.
 * - Expose the database user ID through AuthenticatedUser.
 * - Expose account status through UserDetails.
 *
 * IMPORTANT:
 *
 * The JWT identifies the user by email, but authorization decisions are based
 * on the database-backed authenticated principal.
 *
 * The frontend never supplies the authenticated user's ID.
 *
 * ============================================================================
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class CustomUserDetailsService
        implements UserDetailsService {

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Invalid email or password.";

    private static final String INVALID_ROLE_MESSAGE =
            "User account has an invalid security role.";

    private static final String NO_ROLE_MESSAGE =
            "User account has no assigned role.";

    private final UserRepository userRepository;


    public CustomUserDetailsService(
            UserRepository userRepository
    ) {

        this.userRepository = userRepository;
    }


    // =========================================================================
    // LOAD USER
    // =========================================================================

    /**
     * Loads an authenticated application user by email.
     *
     * @param email authentication username
     *
     * @return strongly typed authenticated principal
     *
     * @throws UsernameNotFoundException when the account does not exist
     */
    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        if (email == null || email.isBlank()) {

            throw new UsernameNotFoundException(
                    INVALID_CREDENTIALS_MESSAGE
            );
        }


        final String normalizedEmail =
                email
                        .trim()
                        .toLowerCase(Locale.ROOT);


        final User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() -> {

                            /*
                             * Do not reveal whether a specific email address
                             * exists to callers.
                             */
                            log.debug(
                                    "Authentication failed: account not found."
                            );

                            return new UsernameNotFoundException(
                                    INVALID_CREDENTIALS_MESSAGE
                            );
                        });


        // =========================================================================
        // USER ID VALIDATION
        // =========================================================================

        final Long userId = user.getId();

        if (userId == null || userId <= 0) {

            log.error(
                    "Security configuration error: authenticated user has invalid database ID."
            );

            throw new UsernameNotFoundException(
                    "User account could not be authenticated."
            );
        }


        // =========================================================================
        // ROLE VALIDATION
        // =========================================================================

        final Role role = user.getRole();

        if (role == null) {

            log.error(
                    "Security configuration error: user has no assigned role. userId={}",
                    userId
            );

            throw new UsernameNotFoundException(
                    NO_ROLE_MESSAGE
            );
        }


        final String authority = role.getAuthority();

        if (
                authority == null
                        ||
                        authority.isBlank()
                        ||
                        !authority.startsWith("ROLE_")
        ) {

            log.error(
                    "Security configuration error: invalid authority for role. userId={} role={}",
                    userId,
                    role.name()
            );

            throw new UsernameNotFoundException(
                    INVALID_ROLE_MESSAGE
            );
        }


        // =========================================================================
        // AUTHORITIES
        // =========================================================================

        final List<SimpleGrantedAuthority> authorities =
                List.of(
                        new SimpleGrantedAuthority(
                                authority
                        )
                );


        // =========================================================================
        // ACCOUNT STATE
        // =========================================================================

        final boolean enabled =
                user.isEnabled();


        log.debug(
                "Security principal loaded successfully | userId={} | role={} | enabled={}",
                userId,
                role.name(),
                enabled
        );


        // =========================================================================
        // STRONGLY TYPED PRINCIPAL
        // =========================================================================

        return new AuthenticatedUser(
                userId,
                user.getEmail(),
                user.getPassword(),
                authorities,
                enabled,
                true,
                true,
                true
        );
    }
}

