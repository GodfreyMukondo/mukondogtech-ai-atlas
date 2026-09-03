package com.godfrey.ai_immigration_document_analyzer.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * ============================================================================
 * CUSTOM USER PRINCIPAL
 * ============================================================================
 *
 * Strongly typed Spring Security principal used throughout the application.
 *
 * The database user ID is stored explicitly in this principal.
 *
 * This prevents controllers and services from attempting to derive the
 * database user ID from authentication.getName(), email addresses, JWT
 * subjects, or other ambiguous values.
 *
 * Security rule:
 *
 *     principal.getId()
 *
 * is the authoritative authenticated database user ID.
 *
 * ============================================================================
 */
@Getter
public final class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Creates an authenticated application principal.
     *
     * @param id authenticated database user ID
     * @param username authenticated username/email
     * @param password password value when available
     * @param authorities granted authorities
     */
    public CustomUserPrincipal(
            Long id,
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Authenticated user ID must be a positive value."
            );
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated username cannot be empty."
            );
        }

        this.id = id;
        this.username = username.trim();
        this.password = password;
        this.authorities =
                authorities == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableCollection(authorities);
    }

    /**
     * Alias retained for code that uses getUserId().
     */
    public Long getUserId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "CustomUserPrincipal{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", authorities=" + authorities +
                '}';
    }
}

