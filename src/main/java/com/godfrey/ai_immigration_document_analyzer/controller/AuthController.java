package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.dto.request.AuthRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.ChangePasswordRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.RegisterRequest;

import com.godfrey.ai_immigration_document_analyzer.dto.response.AuthResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.UserResponse;

import com.godfrey.ai_immigration_document_analyzer.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid
            @RequestBody
            RegisterRequest request
    ) {

        log.info(
                "User registration request received."
        );

        AuthResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Authenticate a user.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @RequestBody
            AuthRequest request
    ) {

        log.info(
                "Authentication login request received."
        );

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(
                response
        );
    }

    /**
     * Change the currently authenticated user's password.
     */
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest request,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        authService.changePassword(
                email,
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                "Password changed successfully."
        );
    }

    /**
     * Stateless logout.
     *
     * The frontend must remove the JWT from local storage/session storage
     * or its in-memory authentication state.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {

        log.debug(
                "Logout request processed."
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    /**
     * Return the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        UserResponse response =
                authService.getCurrentUser(email);

        return ResponseEntity.ok(
                response
        );
    }

    /**
     * Public authentication service status.
     */
    @GetMapping("/status")
    public ResponseEntity<String> status() {

        return ResponseEntity.ok(
                "Authentication service is running."
        );
    }
}

