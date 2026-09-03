package com.godfrey.ai_immigration_document_analyzer.security;

import lombok.Getter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * ============================================================================
 * AUTHENTICATED USER PRINCIPAL
 * ============================================================================
 *
 * Strongly typed Spring Security principal used throughout the application.
 *
 * Responsibilities:
 *
 * - Expose the authenticated database user ID.
 * - Expose the user's email.
 * - Expose Spring Security authorities.
 * - Preserve standard UserDetails account-state information.
 *
 * IMPORTANT:
 *
 * The database user ID is taken from the authenticated User entity loaded
 * by the backend. It is never supplied by the frontend.
 *
 * This principal eliminates the need for reflection-based extraction of
 * user IDs from Authentication objects.
 *
 * ============================================================================
 */
@Getter
public final class AuthenticatedUser implements UserDetails {

    private final Long id;

    private final String email;

    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    private final boolean enabled;

    private final boolean accountNonExpired;

    private final boolean accountNonLocked;

    private final boolean credentialsNonExpired;


    /**
     * Creates an authenticated application user.
     *
     * @param id database user ID
     * @param email normalized user email
     * @param password encoded password
     * @param authorities Spring Security authorities
     * @param enabled whether the account is enabled
     * @param accountNonExpired whether the account has not expired
     * @param accountNonLocked whether the account is not locked
     * @param credentialsNonExpired whether credentials have not expired
     */
    public AuthenticatedUser(
            Long id,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired
    ) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Authenticated user ID must be a positive value."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated user email must not be blank."
            );
        }

        if (authorities == null) {
            throw new IllegalArgumentException(
                    "Authenticated user authorities must not be null."
            );
        }

        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }


    /**
     * Returns the database user ID.
     */
    public Long getUserId() {
        return id;
    }


    /**
     * Spring Security username.
     *
     * The application uses email as the username.
     */
    @Override
    public String getUsername() {
        return email;
    }


    @Override
    public String getPassword() {
        return password;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }


    @Override
    public boolean isEnabled() {
        return enabled;
    }


    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }


    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }


    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }


    @Override
    public String toString() {

        return "AuthenticatedUser{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", authorities=" + authorities +
                ", enabled=" + enabled +
                '}';
    }
}


