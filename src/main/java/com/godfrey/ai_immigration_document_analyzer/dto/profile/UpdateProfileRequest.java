package com.godfrey.ai_immigration_document_analyzer.dto.profile;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * ============================================================================
 * UPDATE PROFILE REQUEST
 * ============================================================================
 *
 * Request DTO for updating the authenticated user's profile.
 *
 * Endpoint usage:
 *
 *     PUT /api/profile/me
 *
 *     or
 *
 *     PATCH /api/profile/me
 *
 * Depending on the controller implementation.
 *
 *
 * Supported profile fields:
 *
 *     - fullName
 *     - email
 *
 *
 * Frontend compatibility:
 *
 * The API accepts both:
 *
 *     {
 *         "fullName": "Godfrey Mukondo"
 *     }
 *
 * and:
 *
 *     {
 *         "name": "Godfrey Mukondo"
 *     }
 *
 *
 * Security-sensitive fields are intentionally NOT included:
 *
 *     - id
 *     - password
 *     - role
 *     - enabled
 *     - mustChangePassword
 *     - authorities
 *     - permissions
 *
 * Those fields must never be modified through the normal profile
 * endpoint.
 *
 *
 * Design principles:
 *
 *     - DTO must not expose the User entity directly.
 *     - Validation is performed before entering the service layer.
 *     - Email validation is handled by Jakarta Bean Validation.
 *     - Name length is constrained to match the User entity.
 *     - Null values are permitted so PATCH-style partial updates remain
 *       possible.
 *     - Empty/blank values are rejected by the service layer when a field
 *       is actually supplied for modification.
 *
 * ============================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProfileRequest {


    /*
    |--------------------------------------------------------------------------
    | FULL NAME
    |--------------------------------------------------------------------------
    */

    /**
     * Full name of the authenticated user.
     *
     * Database/entity field:
     *
     *     User.fullName
     *
     * Maximum length matches the User entity:
     *
     *     @Column(length = 150)
     *
     * The @JsonAlias annotation allows existing frontend code to send:
     *
     *     "name"
     *
     * while the canonical API field remains:
     *
     *     "fullName"
     *
     * Example:
     *
     *     {
     *         "fullName": "Godfrey Mukondo"
     *     }
     *
     * or:
     *
     *     {
     *         "name": "Godfrey Mukondo"
     *     }
     */
    @JsonAlias("name")
    @Size(
            min = 2,
            max = 150,
            message = "Full name must be between 2 and 150 characters."
    )
    private String fullName;


    /*
    |--------------------------------------------------------------------------
    | EMAIL
    |--------------------------------------------------------------------------
    */

    /**
     * Email address of the authenticated user.
     *
     * The User entity normalizes email addresses to lowercase during
     * persistence through its entity lifecycle callback.
     *
     * The service layer should also normalize the value before performing
     * uniqueness checks so that duplicate-email checks are deterministic.
     */
    @Email(
            message = "Please provide a valid email address."
    )
    @Size(
            max = 255,
            message = "Email cannot exceed 255 characters."
    )
    private String email;


    /*
    |--------------------------------------------------------------------------
    | NAME COMPATIBILITY METHOD
    |--------------------------------------------------------------------------
    */

    /**
     * Returns the requested profile name.
     *
     * This method intentionally provides compatibility with existing
     * service code that uses:
     *
     *     request.name()
     *
     * The canonical DTO property remains:
     *
     *     fullName
     *
     * This method is ignored by Jackson so it does not become an additional
     * JSON property.
     *
     * @return requested full name
     */
    @JsonIgnore
    public String name() {

        return fullName;
    }


    /*
    |--------------------------------------------------------------------------
    | NAME SETTER COMPATIBILITY
    |--------------------------------------------------------------------------
    */

    /**
     * Sets the requested profile name.
     *
     * This compatibility method allows application code to use:
     *
     *     request.name("Godfrey Mukondo");
     *
     * without introducing a second database/entity field.
     *
     * Jackson ignores this method as a JSON property.
     *
     * @param name requested full name
     */
    @JsonIgnore
    public void name(
            String name
    ) {

        this.fullName = name;
    }


    /*
    |--------------------------------------------------------------------------
    | FIELD PRESENCE HELPERS
    |--------------------------------------------------------------------------
    */

    /**
     * Determines whether a full-name update was supplied.
     *
     * The service layer can use this method when implementing PATCH-style
     * updates.
     *
     * Blank strings are treated as not usable values and should normally
     * result in validation/business validation failure when supplied.
     *
     * @return true when full name contains a non-blank value
     */
    @JsonIgnore
    public boolean hasFullName() {

        return fullName != null
                && !fullName.trim().isEmpty();
    }


    /**
     * Determines whether an email update was supplied.
     *
     * @return true when email contains a non-blank value
     */
    @JsonIgnore
    public boolean hasEmail() {

        return email != null
                && !email.trim().isEmpty();
    }


    /**
     * Determines whether the request contains at least one profile
     * field that can be updated.
     *
     * This is useful for rejecting an empty PATCH request at the service
     * layer.
     *
     * @return true when at least one supported profile field is supplied
     */
    @JsonIgnore
    public boolean hasChanges() {

        return hasFullName()
                || hasEmail();
    }


    /*
    |--------------------------------------------------------------------------
    | NORMALIZATION HELPERS
    |--------------------------------------------------------------------------
    */

    /**
     * Returns a normalized full name.
     *
     * The User entity also performs trimming before persistence, but
     * normalizing here allows the service layer to work with clean values
     * before performing business validation.
     *
     * @return trimmed full name or null
     */
    @JsonIgnore
    public String normalizedFullName() {

        if (fullName == null) {
            return null;
        }

        String normalized =
                fullName.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }


    /**
     * Returns a normalized email address.
     *
     * Email normalization is deliberately kept deterministic:
     *
     *     trim
     *     lowercase
     *
     * This should be used by the service layer before:
     *
     *     - uniqueness checks
     *     - persistence
     *
     * @return normalized email or null
     */
    @JsonIgnore
    public String normalizedEmail() {

        if (email == null) {
            return null;
        }

        String normalized =
                email.trim()
                        .toLowerCase();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}