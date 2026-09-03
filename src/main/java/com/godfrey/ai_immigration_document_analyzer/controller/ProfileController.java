package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.analytics.service.ProfileService;
import com.godfrey.ai_immigration_document_analyzer.dto.profile.ProfileResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.profile.ProfileUsageResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.profile.UpdateProfileRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * PROFILE CONTROLLER
 * ============================================================================
 *
 * REST API for the authenticated user's profile.
 *
 * Endpoints
 * ---------
 *
 * GET   /api/profile/me
 * PATCH /api/profile/me
 * GET   /api/profile/usage
 *
 * Security
 * --------
 *
 * All operations are scoped to the currently authenticated user.
 *
 * The client never supplies:
 *
 * - user ID
 * - account ID
 * - email belonging to another account
 *
 * The authenticated username is obtained from Spring Security's
 * Authentication object, which is populated by the application's
 * authentication/JWT security layer.
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {


    /*
    |--------------------------------------------------------------------------
    | DEPENDENCIES
    |--------------------------------------------------------------------------
    */

    private final ProfileService profileService;


    /*
    |--------------------------------------------------------------------------
    | GET CURRENT PROFILE
    |--------------------------------------------------------------------------
    */

    /**
     * =========================================================================
     * GET /api/profile/me
     * =========================================================================
     *
     * Returns the profile belonging to the currently authenticated user.
     *
     * Example response:
     *
     * {
     *     "data": {
     *         "id": 1,
     *         "name": "Godfrey Mukondo",
     *         "email": "user@example.com",
     *         "role": "USER",
     *         "plan": "FREE",
     *         "analysesUsed": 0,
     *         "analysesLimit": 0,
     *         "memberSince": "2026-08-20T00:00:00Z",
     *         "verified": false,
     *         "avatarUrl": null
     *     }
     * }
     *
     * @param authentication authenticated Spring Security principal
     * @return authenticated user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileApiResponse<ProfileResponse>> getMyProfile(
            Authentication authentication
    ) {

        String username = requireAuthenticatedUsername(
                authentication
        );

        log.debug(
                "Fetching profile for authenticated user."
        );

        ProfileResponse profile =
                profileService.getCurrentProfile(
                        username
                );

        return ResponseEntity.ok(
                ProfileApiResponse.success(
                        profile
                )
        );
    }


    /*
    |--------------------------------------------------------------------------
    | GET CURRENT USAGE
    |--------------------------------------------------------------------------
    */

    /**
     * =========================================================================
     * GET /api/profile/usage
     * =========================================================================
     *
     * Returns AI analysis usage for the currently authenticated user.
     *
     * Example response:
     *
     * {
     *     "data": {
     *         "used": 5,
     *         "limit": 20,
     *         "remaining": 15,
     *         "percentage": 25.0,
     *         "unlimited": false
     *     }
     * }
     *
     * @param authentication authenticated Spring Security principal
     * @return authenticated user's usage information
     */
    @GetMapping("/usage")
    public ResponseEntity<ProfileApiResponse<ProfileUsageResponse>> getMyUsage(
            Authentication authentication
    ) {

        String username = requireAuthenticatedUsername(
                authentication
        );

        log.debug(
                "Fetching profile usage for authenticated user."
        );

        ProfileUsageResponse usage =
                profileService.getCurrentUsage(
                        username
                );

        return ResponseEntity.ok(
                ProfileApiResponse.success(
                        usage
                )
        );
    }


    /*
    |--------------------------------------------------------------------------
    | UPDATE CURRENT PROFILE
    |--------------------------------------------------------------------------
    */

    /**
     * =========================================================================
     * PATCH /api/profile/me
     * =========================================================================
     *
     * Updates the authenticated user's permitted profile fields.
     *
     * The request is intentionally restricted to UpdateProfileRequest.
     *
     * The following fields cannot be changed through this endpoint:
     *
     * - id
     * - password
     * - role
     * - enabled
     * - mustChangePassword
     * - security configuration
     *
     * Example request:
     *
     * {
     *     "fullName": "Godfrey Mukondo",
     *     "email": "godfrey@example.com"
     * }
     *
     * The DTO also supports:
     *
     * {
     *     "name": "Godfrey Mukondo"
     * }
     *
     * through its @JsonAlias configuration.
     *
     * @param authentication authenticated Spring Security principal
     * @param request validated profile update request
     * @return updated authenticated user's profile
     */
    @PatchMapping("/me")
    public ResponseEntity<ProfileApiResponse<ProfileResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        String username = requireAuthenticatedUsername(
                authentication
        );

        if (request == null) {

            throw new IllegalArgumentException(
                    "Profile update request cannot be null."
            );
        }

        log.info(
                "Updating profile for authenticated user."
        );

        ProfileResponse profile =
                profileService.updateCurrentProfile(
                        username,
                        request
                );

        return ResponseEntity.ok(
                ProfileApiResponse.success(
                        profile
                )
        );
    }


    /*
    |--------------------------------------------------------------------------
    | AUTHENTICATION VALIDATION
    |--------------------------------------------------------------------------
    */

    /**
     * Ensures that the request contains a valid authenticated principal.
     *
     * This provides an additional defensive layer before calling the service.
     *
     * Normally Spring Security should prevent unauthenticated requests from
     * reaching these endpoints.
     *
     * @param authentication Spring Security authentication object
     * @return authenticated username
     */
    private String requireAuthenticatedUsername(
            Authentication authentication
    ) {

        if (authentication == null) {

            log.warn(
                    "Profile endpoint accessed without authentication."
            );

            throw new org.springframework.security.authentication
                    .AuthenticationCredentialsNotFoundException(
                    "Authentication is required."
            );
        }

        if (!authentication.isAuthenticated()) {

            log.warn(
                    "Profile endpoint accessed with unauthenticated principal."
            );

            throw new org.springframework.security.authentication
                    .AuthenticationCredentialsNotFoundException(
                    "Authentication is required."
            );
        }

        String username = authentication.getName();

        if (username == null ||
                username.isBlank()) {

            log.warn(
                    "Authenticated principal does not contain a username."
            );

            throw new org.springframework.security.core.userdetails
                    .UsernameNotFoundException(
                    "Authenticated user could not be identified."
            );
        }

        return username.trim();
    }


    /*
    |--------------------------------------------------------------------------
    | API RESPONSE ENVELOPE
    |--------------------------------------------------------------------------
    */

    /**
     * =========================================================================
     * PROFILE API RESPONSE
     * =========================================================================
     *
     * Generic response envelope used by the profile API.
     *
     * Keeping the response structure consistent makes frontend integration
     * simpler and allows additional metadata to be introduced later without
     * changing the top-level response contract.
     *
     * Current response:
     *
     * {
     *     "data": { ... }
     * }
     *
     * =========================================================================
     */
    public record ProfileApiResponse<T>(
            T data
    ) {

        /**
         * Creates a successful API response.
         *
         * @param data response payload
         * @param <T> payload type
         * @return response envelope
         */
        public static <T> ProfileApiResponse<T> success(
                T data
        ) {

            return new ProfileApiResponse<>(
                    data
            );
        }
    }
}