package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.request.AuthRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.RegisterRequest;

import com.godfrey.ai_immigration_document_analyzer.dto.response.AuthResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.UserResponse;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;

import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;

import com.godfrey.ai_immigration_document_analyzer.security.JwtService;

import com.godfrey.ai_immigration_document_analyzer.service.validation.PasswordPolicyValidator;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@Slf4j
public class AuthService {

    private static final String INVALID_CREDENTIALS =
            "Invalid email or password.";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final PasswordPolicyValidator passwordPolicyValidator;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordPolicyValidator passwordPolicyValidator
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /**
     * Register a normal USER account.
     */
    @Transactional
    public AuthResponse register(
            RegisterRequest request
    ) {

        validateRegisterRequest(request);

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        if (
                userRepository.existsByEmailIgnoreCase(
                        email
                )
        ) {

            log.debug(
                    "Registration rejected because email already exists."
            );

            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        User user =
                User.builder()
                        .fullName(
                                request.getFullName()
                                        .trim()
                        )
                        .email(email)
                        .password(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .role(Role.USER)
                        .enabled(true)
                        .mustChangePassword(false)
                        .build();

        User savedUser =
                userRepository.save(user);

        log.info(
                "New user registered successfully. role={}",
                savedUser.getRole()
        );

        String token =
                jwtService.generateToken(
                        savedUser
                );

        return buildAuthResponse(
                token,
                savedUser
        );
    }

    /**
     * Authenticate user credentials and issue JWT.
     */
    @Transactional
    public AuthResponse login(
            AuthRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Login request cannot be null."
            );
        }

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> {

                            log.debug(
                                    "Authentication failed."
                            );

                            return new BadCredentialsException(
                                    INVALID_CREDENTIALS
                            );
                        });

        if (!user.isEnabled()) {

            log.debug(
                    "Authentication rejected because account is disabled."
            );

            throw new BadCredentialsException(
                    INVALID_CREDENTIALS
            );
        }

        if (
                user.getPassword() == null
                        ||
                        user.getPassword().isBlank()
        ) {

            log.error(
                    "Authentication configuration error: user has no password hash."
            );

            throw new BadCredentialsException(
                    INVALID_CREDENTIALS
            );
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {

            log.debug(
                    "Authentication failed."
            );

            throw new BadCredentialsException(
                    INVALID_CREDENTIALS
            );
        }

        String token =
                jwtService.generateToken(
                        user
                );

        log.info(
                "Login successful. role={}",
                user.getRole()
        );

        return buildAuthResponse(
                token,
                user
        );
    }

    /**
     * Get the authenticated user's profile.
     */
    @Transactional
    public UserResponse getCurrentUser(
            String email
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Authenticated user does not exist."
                                )
                        );

        return mapUser(user);
    }

    /**
     * Change authenticated user's password.
     */
    @Transactional
    public void changePassword(
            String email,
            String newPassword
    ) {

        validatePassword(newPassword);

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizeEmail(email)
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "User does not exist."
                                )
                        );

        user.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
        );

        user.completePasswordChange();

        userRepository.save(user);

        log.info(
                "Password changed successfully."
        );
    }

    /**
     * Build authentication response.
     */
    private AuthResponse buildAuthResponse(
            String token,
            User user
    ) {

        return AuthResponse.builder()
                .token(token)
                .user(
                        mapUser(user)
                )
                .build();
    }

    /**
     * Map entity to safe response DTO.
     */
    private UserResponse mapUser(
            User user
    ) {

        if (user == null) {

            throw new IllegalArgumentException(
                    "User cannot be null."
            );
        }

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(
                        normalizeRole(
                                user.getRole()
                        )
                )
                .enabled(
                        user.isEnabled()
                )
                .mustChangePassword(
                        user.isMustChangePassword()
                )
                .build();
    }

    /**
     * Validate registration.
     */
    private void validateRegisterRequest(
            RegisterRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Registration request cannot be null."
            );
        }

        if (
                request.getFullName() == null
                        ||
                        request.getFullName().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Full name is required."
            );
        }

        if (
                request.getEmail() == null
                        ||
                        request.getEmail().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        validatePassword(
                request.getPassword()
        );
    }

    /**
     * Central password policy.
     *
     * Delegates to the shared {@link PasswordPolicyValidator} so the
     * register and change-password flows enforce the exact same rules
     * (length, uppercase, digit) as the forgot-password flow and the
     * frontend's client-side validation.
     */
    private void validatePassword(
            String password
    ) {

        passwordPolicyValidator.validate(password);
    }

    /**
     * Normalize email consistently.
     */
    private String normalizeEmail(
            String email
    ) {

        if (
                email == null
                        ||
                        email.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Email cannot be empty."
            );
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Normalize role for API response.
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
                .toUpperCase(Locale.ROOT);
    }
}

