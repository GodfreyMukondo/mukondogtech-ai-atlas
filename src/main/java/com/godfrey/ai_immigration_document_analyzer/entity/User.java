package com.godfrey.ai_immigration_document_analyzer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Entity
@Table(
        name = "USERS",

        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_USERS_EMAIL",
                        columnNames = "EMAIL"
                )
        },

        indexes = {
                @Index(
                        name = "IDX_USERS_EMAIL",
                        columnList = "EMAIL"
                ),
                @Index(
                        name = "IDX_USERS_ROLE",
                        columnList = "ROLE"
                ),
                @Index(
                        name = "IDX_USERS_ENABLED",
                        columnList = "ENABLED"
                ),
                @Index(
                        name = "IDX_USERS_CREATED_AT",
                        columnList = "CREATED_AT"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class User implements UserDetails {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "users_seq"
    )
    @SequenceGenerator(
            name = "users_seq",
            sequenceName = "USERS_SEQ",
            allocationSize = 1
    )
    @Column(
            name = "ID",
            nullable = false
    )
    private Long id;

    @Column(
            name = "FULL_NAME",
            nullable = false,
            length = 150
    )
    private String fullName;

    @Column(
            name = "EMAIL",
            nullable = false,
            length = 255
    )
    private String email;

    @Column(
            name = "PHONE",
            length = 30
    )
    private String phone;

    @Column(
            name = "COUNTRY",
            length = 100
    )
    private String country;

    @JsonIgnore
    @Column(
            name = "PASSWORD",
            nullable = false,
            length = 255
    )
    @ToString.Exclude
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "ROLE",
            nullable = false,
            length = 50
    )
    @Builder.Default
    private Role role = Role.USER;

    @Column(
            name = "ENABLED",
            nullable = false
    )
    @Builder.Default
    private boolean enabled = true;

    @Column(
            name = "MUST_CHANGE_PASSWORD",
            nullable = false
    )
    @Builder.Default
    private boolean mustChangePassword = false;

    @CreationTimestamp
    @Column(
            name = "CREATED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "UPDATED_AT"
    )
    private LocalDateTime updatedAt;

    /**
     * Spring Security authorities.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Role effectiveRole =
                role != null
                        ? role
                        : Role.USER;

        return List.of(
                new SimpleGrantedAuthority(
                        effectiveRole.getAuthority()
                )
        );
    }

    /**
     * Email is the authentication username.
     */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Account expiration is not currently implemented.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account locking is not currently implemented.
     *
     * Do not use enabled for both enabled and locked state.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credential expiration is not currently implemented.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Application account status.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Normalize persistent fields.
     */
    @PrePersist
    @PreUpdate
    private void normalizeFields() {

        if (email != null) {

            email =
                    email
                            .trim()
                            .toLowerCase(Locale.ROOT);
        }

        if (fullName != null) {

            fullName =
                    fullName
                            .trim()
                            .replaceAll(
                                    "\\s+",
                                    " "
                            );
        }

        if (role == null) {
            role = Role.USER;
        }

        if (phone != null) {
            phone = phone.trim();
        }

        if (country != null) {
            country = country.trim();
        }
    }

    public void activateAccount() {
        this.enabled = true;
    }

    public void deactivateAccount() {
        this.enabled = false;
    }

    public void changeRole(
            Role newRole
    ) {

        if (newRole == null) {
            throw new IllegalArgumentException(
                    "Role cannot be null."
            );
        }

        this.role = newRole;
    }

    public void requirePasswordChange() {
        this.mustChangePassword = true;
    }

    public void completePasswordChange() {
        this.mustChangePassword = false;
    }

    public boolean canAccessApplication() {
        return enabled && !mustChangePassword;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isRegularUser() {
        return role == Role.USER;
    }

    public String getAuthority() {

        Role effectiveRole =
                role != null
                        ? role
                        : Role.USER;

        return effectiveRole.getAuthority();
    }

    @Override
    public String toString() {

        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", enabled=" + enabled +
                ", mustChangePassword=" + mustChangePassword +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

