package com.godfrey.ai_immigration_document_analyzer.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
public enum Role {

    USER,

    ADMIN,

    /**
     * Authorized to access Fact/case data for subjects they are explicitly
     * assigned to (see {@code CaseAssignment}). Deliberately distinct from
     * ADMIN: platform administration privileges and case-data access
     * privileges are separate grants - holding ADMIN alone does not confer
     * CASE_WORKER access, and vice versa.
     */
    CASE_WORKER;

    /**
     * Converts the application role to a Spring Security authority.
     */
    public String getAuthority() {
        return "ROLE_" + name();
    }

    /**
     * JSON representation.
     */
    @JsonValue
    public String jsonValue() {
        return name();
    }

    /**
     * JSON/request deserialization.
     */
    @JsonCreator
    public static Role fromString(
            String value
    ) {
        return fromAuthority(value);
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isUser() {
        return this == USER;
    }

    /**
     * Resolves USER / ADMIN / ROLE_USER / ROLE_ADMIN.
     */
    public static Role fromAuthority(
            String authority
    ) {

        if (
                authority == null
                        ||
                        authority.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Role authority cannot be null or blank."
            );
        }

        String normalized =
                authority
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.startsWith("ROLE_")) {

            normalized =
                    normalized.substring(
                            "ROLE_".length()
                    );
        }

        try {

            return Role.valueOf(
                    normalized
            );

        } catch (IllegalArgumentException exception) {

            log.warn(
                    "Unknown security role received."
            );

            throw new IllegalArgumentException(
                    "Unknown security role: " + authority,
                    exception
            );
        }
    }

    /**
     * Parses a database role value.
     */
    public static Role fromDatabaseValue(
            String value
    ) {
        return fromAuthority(value);
    }
}

