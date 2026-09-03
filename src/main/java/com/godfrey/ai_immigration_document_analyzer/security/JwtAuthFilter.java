package com.godfrey.ai_immigration_document_analyzer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================================
 * JWT AUTHENTICATION FILTER
 * ============================================================================
 *
 * Authenticates incoming requests using a Bearer JWT.
 *
 * Authentication flow:
 *
 * 1. Read Authorization: Bearer <token>.
 * 2. Validate the JWT.
 * 3. Extract the email/subject from the JWT.
 * 4. Load the current user from the database.
 * 5. Validate account state.
 * 6. Create an AuthenticatedUser principal containing the database ID.
 * 7. Store the authentication in SecurityContext.
 *
 * IMPORTANT:
 *
 * The database user ID is NOT taken from the frontend and is NOT blindly
 * trusted from an arbitrary JWT claim.
 *
 * The authenticated principal is rebuilt from the current database user.
 *
 * ============================================================================
 */
@Component
@Slf4j
public class JwtAuthFilter
        extends OncePerRequestFilter {


    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;


    public JwtAuthFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }


    // =========================================================================
    // FILTER
    // =========================================================================

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String requestUri =
                request.getRequestURI();

        final String method =
                request.getMethod();


        try {

            // =================================================================
            // EXISTING AUTHENTICATION
            // =================================================================

            final Authentication existingAuthentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();


            if (
                    existingAuthentication != null
                            &&
                            existingAuthentication.isAuthenticated()
                            &&
                            !isAnonymousAuthentication(
                                    existingAuthentication
                            )
            ) {

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =================================================================
            // EXTRACT TOKEN
            // =================================================================

            final String token =
                    extractToken(request);


            if (!StringUtils.hasText(token)) {

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =================================================================
            // VALIDATE TOKEN
            // =================================================================

            if (!jwtService.isTokenValid(token)) {

                log.debug(
                        "JWT validation failed | method={} | endpoint={}",
                        method,
                        requestUri
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =================================================================
            // EXTRACT SUBJECT
            // =================================================================

            final String email =
                    jwtService.extractUsername(token);


            if (!StringUtils.hasText(email)) {

                log.debug(
                        "JWT does not contain a valid subject | method={} | endpoint={}",
                        method,
                        requestUri
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =================================================================
            // LOAD CURRENT USER
            // =================================================================

            final UserDetails userDetails =
                    userDetailsService.loadUserByUsername(
                            email.trim()
                    );


            // =================================================================
            // ACCOUNT VALIDATION
            // =================================================================

            if (
                    !userDetails.isEnabled()
                            ||
                            !userDetails.isAccountNonExpired()
                            ||
                            !userDetails.isAccountNonLocked()
                            ||
                            !userDetails.isCredentialsNonExpired()
            ) {

                log.debug(
                        "JWT rejected because authenticated account is not valid."
                );

                SecurityContextHolder.clearContext();

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =================================================================
            // CREATE AUTHENTICATION
            // =================================================================

            final UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );


            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );


            // =================================================================
            // SECURITY CONTEXT
            // =================================================================

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(
                            authentication
                    );


            /*
             * Because CustomUserDetailsService returns AuthenticatedUser,
             * the SecurityContext now contains the database user ID.
             */
            if (userDetails instanceof AuthenticatedUser authenticatedUser) {

                log.debug(
                        "JWT authentication successful | userId={} | endpoint={} | authorities={}",
                        authenticatedUser.getUserId(),
                        requestUri,
                        authenticatedUser.getAuthorities()
                );

            } else {

                /*
                 * Defensive check. This should never happen unless another
                 * UserDetailsService implementation is substituted.
                 */
                log.warn(
                        "Authentication succeeded with unexpected principal type: {}",
                        userDetails.getClass().getName()
                );
            }


        } catch (Exception exception) {

            /*
             * Never leave a partially authenticated context behind.
             */
            SecurityContextHolder.clearContext();


            log.debug(
                    "JWT processing failed | method={} | endpoint={} | exceptionType={} | message={}",
                    method,
                    requestUri,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }


        filterChain.doFilter(
                request,
                response
        );
    }


    // =========================================================================
    // ANONYMOUS AUTHENTICATION
    // =========================================================================

    private boolean isAnonymousAuthentication(
            Authentication authentication
    ) {

        return authentication
                instanceof AnonymousAuthenticationToken;
    }


    // =========================================================================
    // TOKEN EXTRACTION
    // =========================================================================

    /**
     * Extracts a Bearer token from the Authorization header.
     */
    private String extractToken(
            HttpServletRequest request
    ) {

        final String authorizationHeader =
                request.getHeader(
                        HttpHeaders.AUTHORIZATION
                );


        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }


        if (
                !authorizationHeader.regionMatches(
                        true,
                        0,
                        "Bearer ",
                        0,
                        7
                )
        ) {

            return null;
        }


        final String token =
                authorizationHeader
                        .substring(7)
                        .trim();


        return StringUtils.hasText(token)
                ? token
                : null;
    }


    // =========================================================================
    // PUBLIC ENDPOINTS
    // =========================================================================

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        final String path =
                request.getServletPath();


        if (!StringUtils.hasText(path)) {
            return false;
        }


        // ---------------------------------------------------------------------
        // Authentication endpoints
        // ---------------------------------------------------------------------

        if (
                path.equals("/api/auth/login")
                        ||
                        path.equals("/api/auth/register")
                        ||
                        path.equals("/api/auth/verify-email")
                        ||
                        path.equals("/api/auth/forgot-password")
                        ||
                        path.equals("/api/auth/reset-password")
                        ||
                        path.equals("/api/auth/status")
        ) {

            return true;
        }


        // ---------------------------------------------------------------------
        // Swagger / OpenAPI
        // ---------------------------------------------------------------------

        if (
                path.startsWith("/swagger-ui")
                        ||
                        path.startsWith("/v3/api-docs")
        ) {

            return true;
        }


        // ---------------------------------------------------------------------
        // Health
        // ---------------------------------------------------------------------

        if (path.equals("/actuator/health")) {
            return true;
        }


        // ---------------------------------------------------------------------
        // Spring error endpoint
        // ---------------------------------------------------------------------

        return path.equals("/error");
    }
}

