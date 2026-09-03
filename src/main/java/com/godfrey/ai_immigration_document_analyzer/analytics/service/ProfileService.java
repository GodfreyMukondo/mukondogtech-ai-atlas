package com.godfrey.ai_immigration_document_analyzer.analytics.service;

import com.godfrey.ai_immigration_document_analyzer.dto.profile.ProfileResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.profile.ProfileUsageResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.profile.UpdateProfileRequest;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;


/**
 * ============================================================================
 * PROFILE SERVICE
 * ============================================================================
 *
 * Application service responsible for authenticated-user profile operations.
 *
 * Responsibilities:
 *
 * - Retrieve the currently authenticated user's profile.
 * - Update permitted profile fields.
 * - Validate authenticated-user identity.
 * - Normalize profile information.
 * - Enforce email uniqueness.
 * - Prevent profile operations against disabled accounts.
 * - Calculate profile usage information.
 * - Convert User entities into safe API DTOs.
 *
 *
 * Security:
 *
 * The service NEVER accepts a user ID from the frontend for normal profile
 * operations.
 *
 * The authenticated user's email/username is obtained from Spring Security.
 *
 *
 * Supported profile fields:
 *
 * - Full name
 * - Email
 *
 *
 * Deliberately protected fields:
 *
 * - ID
 * - Password
 * - Role
 * - Enabled
 * - Must-change-password
 *
 * Those fields must be managed by dedicated authentication or
 * administration services.
 *
 *
 * Database:
 *
 * Oracle Database 21c
 *
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProfileService {


    /*
    |--------------------------------------------------------------------------
    | DEPENDENCIES
    |--------------------------------------------------------------------------
    */

    private final UserRepository userRepository;


    /*
    |--------------------------------------------------------------------------
    | CURRENT PROFILE
    |--------------------------------------------------------------------------
    */

    /**
     * Returns the profile of the currently authenticated user.
     *
     * The authenticated username is expected to be the user's email address,
     * because User#getUsername() returns email.
     *
     * @param authenticatedUsername authenticated Spring Security username
     *
     * @return safe public profile response
     *
     * @throws UsernameNotFoundException when the authenticated account
     *                                   cannot be identified
     */
    public ProfileResponse getCurrentProfile(
            String authenticatedUsername
    ) {

        User user =
                findAuthenticatedUser(
                        authenticatedUsername
                );

        return toProfileResponse(user);
    }


    /*
    |--------------------------------------------------------------------------
    | CURRENT USAGE
    |--------------------------------------------------------------------------
    */

    /**
     * Returns AI analysis usage for the authenticated user.
     *
     * IMPORTANT:
     *
     * Usage values must ultimately come from trusted server-side
     * persistence/subscription data.
     *
     * The frontend must never be trusted to provide usage counts.
     *
     * The current User entity supplied by the application does not contain
     * analysis usage or subscription-limit fields. Therefore this service
     * keeps the usage calculation isolated in dedicated methods so that the
     * real document-analysis/subscription repositories can be connected
     * without changing the profile API contract.
     *
     * @param authenticatedUsername authenticated Spring Security username
     *
     * @return usage information
     */
    public ProfileUsageResponse getCurrentUsage(
            String authenticatedUsername
    ) {

        User user =
                findAuthenticatedUser(
                        authenticatedUsername
                );


        long used =
                Math.max(
                        getAnalysesUsed(user),
                        0L
                );


        long limit =
                Math.max(
                        getAnalysesLimit(user),
                        0L
                );


        /*
         * According to the existing frontend contract:
         *
         * limit == 0 means unlimited / not configured.
         */
        boolean unlimited =
                limit == 0L;


        long remaining =
                unlimited
                        ? 0L
                        : Math.max(
                        limit - used,
                        0L
                );


        double percentage;


        if (unlimited) {

            percentage = 0.0;

        } else {

            percentage =
                    Math.min(
                            Math.max(
                                    ((double) used / (double) limit)
                                            * 100.0,
                                    0.0
                            ),
                            100.0
                    );
        }


        return new ProfileUsageResponse(
                used,
                limit,
                remaining,
                percentage,
                unlimited
        );
    }


    /*
    |--------------------------------------------------------------------------
    | UPDATE CURRENT PROFILE
    |--------------------------------------------------------------------------
    */

    /**
     * Updates permitted profile fields for the authenticated user.
     *
     * Currently supported:
     *
     * - fullName
     * - name (frontend compatibility alias)
     * - email
     *
     * Security-sensitive fields are deliberately ignored because they are
     * not part of UpdateProfileRequest.
     *
     * @param authenticatedUsername authenticated Spring Security username
     * @param request profile update request
     *
     * @return updated profile
     */
    @Transactional
    public ProfileResponse updateCurrentProfile(
            String authenticatedUsername,
            UpdateProfileRequest request
    ) {

        User user =
                findAuthenticatedUser(
                        authenticatedUsername
                );


        /*
         * =====================================================================
         * REQUEST VALIDATION
         * =====================================================================
         */

        if (request == null) {

            throw new IllegalArgumentException(
                    "Profile update request cannot be null."
            );
        }


        if (!request.hasChanges()) {

            throw new IllegalArgumentException(
                    "At least one profile field must be provided."
            );
        }


        /*
         * =====================================================================
         * FULL NAME
         * =====================================================================
         *
         * IMPORTANT:
         *
         * User contains:
         *
         *     fullName
         *
         * NOT:
         *
         *     name
         *
         * Therefore the correct entity setter is:
         *
         *     user.setFullName(...)
         */

        if (request.hasFullName()) {

            String normalizedName =
                    request.normalizedFullName();


            if (normalizedName == null) {

                throw new IllegalArgumentException(
                        "Full name cannot be blank."
                );
            }


            if (normalizedName.length() < 2 ||
                    normalizedName.length() > 150) {

                throw new IllegalArgumentException(
                        "Full name must contain between 2 and 150 characters."
                );
            }


            user.setFullName(
                    normalizedName
            );
        }


        /*
         * =====================================================================
         * EMAIL
         * =====================================================================
         *
         * Email changes require a uniqueness check.
         *
         * The current authenticated user's own email is allowed to remain
         * unchanged.
         */

        if (request.hasEmail()) {

            String normalizedEmail =
                    request.normalizedEmail();


            if (normalizedEmail == null) {

                throw new IllegalArgumentException(
                        "Email address cannot be blank."
                );
            }


            if (normalizedEmail.length() > 255) {

                throw new IllegalArgumentException(
                        "Email address cannot exceed 255 characters."
                );
            }


            boolean emailChanged =
                    user.getEmail() == null ||
                            !normalizedEmail.equalsIgnoreCase(
                                    user.getEmail()
                            );


            if (emailChanged) {

                boolean alreadyUsed =
                        userRepository.existsByEmailAndIdNot(
                                normalizedEmail,
                                user.getId()
                        );


                if (alreadyUsed) {

                    throw new IllegalArgumentException(
                            "An account with this email address already exists."
                    );
                }


                user.setEmail(
                        normalizedEmail
                );
            }
        }


        /*
         * =====================================================================
         * PERSIST
         * =====================================================================
         */

        User savedUser =
                userRepository.save(
                        user
                );


        log.info(
                "Authenticated user profile updated successfully. userId={}",
                savedUser.getId()
        );


        return toProfileResponse(
                savedUser
        );
    }


    /*
    |--------------------------------------------------------------------------
    | FIND AUTHENTICATED USER
    |--------------------------------------------------------------------------
    */

    /**
     * Finds the authenticated user using the Spring Security username.
     *
     * The User entity uses email as getUsername(), therefore the repository
     * lookup is performed using email.
     *
     * The repository already provides:
     *
     *     findByEmail(...)
     *
     * We intentionally do NOT call findByEmailIgnoreCase(...) because that
     * method does not exist in the current repository.
     */
    private User findAuthenticatedUser(
            String authenticatedUsername
    ) {

        if (authenticatedUsername == null ||
                authenticatedUsername.isBlank()) {

            throw new UsernameNotFoundException(
                    "Authenticated user could not be identified."
            );
        }


        String normalizedEmail =
                authenticatedUsername
                        .trim()
                        .toLowerCase();


        User user =
                userRepository
                        .findByEmail(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "Authenticated user account was not found."
                                )
                        );


        /*
         * Disabled accounts should not be treated as active application
         * users for normal profile operations.
         */
        if (!user.isEnabled()) {

            throw new UsernameNotFoundException(
                    "Authenticated user account is disabled."
            );
        }


        return user;
    }


    /*
    |--------------------------------------------------------------------------
    | ENTITY -> PROFILE RESPONSE
    |--------------------------------------------------------------------------
    */

    /**
     * Converts a User entity into the public ProfileResponse DTO.
     *
     * Sensitive information is intentionally excluded.
     *
     * NEVER expose:
     *
     * - password
     * - security credentials
     * - JWT information
     * - internal authorities
     * - account-management internals
     */
    private ProfileResponse toProfileResponse(
            User user
    ) {

        return new ProfileResponse(
                user.getId(),

                resolveName(
                        user
                ),

                user.getEmail(),

                resolveRole(
                        user
                ),

                resolvePlan(
                        user
                ),

                getAnalysesUsed(
                        user
                ),

                getAnalysesLimit(
                        user
                ),

                resolveMemberSince(
                        user
                ),

                resolveVerified(
                        user
                ),

                resolveAvatarUrl(
                        user
                )
        );
    }


    /*
    |--------------------------------------------------------------------------
    | PROFILE NAME
    |--------------------------------------------------------------------------
    */

    /**
     * Resolves the public profile name.
     *
     * IMPORTANT:
     *
     * The User entity uses:
     *
     *     fullName
     *
     * not:
     *
     *     name
     */
    private String resolveName(
            User user
    ) {

        if (user.getFullName() != null &&
                !user.getFullName().isBlank()) {

            return user.getFullName().trim();
        }


        return "User";
    }


    /*
    |--------------------------------------------------------------------------
    | ROLE
    |--------------------------------------------------------------------------
    */

    /**
     * Resolves the user's public role.
     */
    private String resolveRole(
            User user
    ) {

        if (user.getRole() == null) {

            return "";
        }


        return user.getRole().name();
    }


    /*
    |--------------------------------------------------------------------------
    | SUBSCRIPTION PLAN
    |--------------------------------------------------------------------------
    */

    /**
     * Resolves the user's subscription plan.
     *
     * The current User entity supplied does not contain a subscription
     * relationship or subscriptionPlan field.
     *
     * Until the subscription service/repository is connected, FREE is used
     * as the safe default expected by the existing frontend contract.
     *
     * IMPORTANT:
     *
     * This should eventually delegate to the application's subscription
     * service rather than being treated as permanent business logic.
     */
    private String resolvePlan(
            User user
    ) {

        return "FREE";
    }


    /*
    |--------------------------------------------------------------------------
    | ANALYSIS USAGE
    |--------------------------------------------------------------------------
    */

    /**
     * Returns the number of AI analyses consumed by the user.
     *
     * The current User entity does not contain an analysesUsed field.
     *
     * Therefore this method is intentionally isolated until the existing
     * document/analysis persistence layer is connected.
     *
     * The frontend must never be allowed to submit this value.
     */
    private long getAnalysesUsed(
            User user
    ) {

        /*
         * Connect this to the trusted server-side analysis/document
         * repository when the usage entity is available.
         *
         * Example future implementation:
         *
         * return documentRepository.countByUserId(user.getId());
         */

        return 0L;
    }


    /*
    |--------------------------------------------------------------------------
    | ANALYSIS LIMIT
    |--------------------------------------------------------------------------
    */

    /**
     * Returns the user's analysis limit.
     *
     * The current User entity does not contain subscription-limit data.
     *
     * A value of 0 follows the existing frontend contract:
     *
     *     0 = unlimited / not configured.
     */
    private long getAnalysesLimit(
            User user
    ) {

        /*
         * Connect this to the subscription service when subscription
         * persistence is available.
         */

        return 0L;
    }


    /*
    |--------------------------------------------------------------------------
    | MEMBER SINCE
    |--------------------------------------------------------------------------
    */

    /**
     * Converts the User createdAt timestamp to Instant for the API.
     *
     * User.createdAt is LocalDateTime.
     *
     * Because the application stores audit timestamps as UTC-oriented
     * application timestamps, UTC is used when converting to Instant.
     */
    private Instant resolveMemberSince(
            User user
    ) {

        if (user.getCreatedAt() == null) {

            return null;
        }


        return user.getCreatedAt()
                .toInstant(
                        ZoneOffset.UTC
                );
    }


    /*
    |--------------------------------------------------------------------------
    | EMAIL VERIFICATION
    |--------------------------------------------------------------------------
    */

    /**
     * Resolves email verification status.
     *
     * IMPORTANT:
     *
     * The supplied User entity currently does NOT contain:
     *
     *     emailVerified
     *
     * or:
     *
     *     verified
     *
     * Therefore this method cannot call a nonexistent User getter.
     *
     * The safe default is false until email-verification persistence is
     * implemented.
     *
     * When the verification field is added to User, replace this method with
     * the real persisted value.
     */
    private boolean resolveVerified(
            User user
    ) {

        return false;
    }


    /*
    |--------------------------------------------------------------------------
    | AVATAR
    |--------------------------------------------------------------------------
    */

    /**
     * Resolves profile avatar URL.
     *
     * The current User entity does not contain an avatar/profile-image field.
     *
     * Therefore null is returned until profile-image storage is implemented.
     *
     * This is preferable to calling a nonexistent User#getAvatarUrl()
     * method or inventing a database field.
     */
    private String resolveAvatarUrl(
            User user
    ) {

        return null;
    }
}