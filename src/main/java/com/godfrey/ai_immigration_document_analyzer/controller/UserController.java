package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateUserRequest;

import com.godfrey.ai_immigration_document_analyzer.dto.response.UserManagementResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.UserStatisticsResponse;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.UserStatus;

import com.godfrey.ai_immigration_document_analyzer.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import org.springframework.security.access.prepost.PreAuthorize;


import org.springframework.web.bind.annotation.*;



/**
 * ============================================================
 * USER MANAGEMENT CONTROLLER
 * ============================================================
 *
 * Enterprise administrative user management API.
 *
 * Responsibilities:
 *
 * - User listing
 * - User search
 * - User filtering
 * - Pagination
 * - User creation
 * - User update
 * - Account activation
 * - Account suspension
 * - User deletion
 * - User statistics
 *
 *
 * Security:
 *
 * ADMIN access only.
 *
 * Database:
 *
 * Oracle Database 21c
 *
 * ============================================================
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class UserController {



    private final UserService userService;





    /**
     * ============================================================
     * GET ALL USERS
     * ============================================================
     *
     * GET /api/users
     *
     * Supports:
     *
     * ?search=
     * ?role=
     * ?status=
     * ?page=
     * ?size=
     *
     */
    @GetMapping
    public ResponseEntity<Page<UserManagementResponse>> getUsers(

            @RequestParam(required = false)
            String search,


            @RequestParam(required = false)
            Role role,


            @RequestParam(required = false)
            UserStatus status,


            @RequestParam(defaultValue = "0")
            int page,


            @RequestParam(defaultValue = "20")
            int size

    ) {


        if(page < 0) {
            page = 0;
        }


        if(size <= 0 || size > 100) {
            size = 20;
        }



        log.debug(
                "Fetching users search={}, role={}, status={}, page={}, size={}",
                search,
                role,
                status,
                page,
                size
        );



        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );



        return ResponseEntity.ok(

                userService.getUsers(
                        search,
                        role,
                        status,
                        pageable
                )

        );

    }









    /**
     * ============================================================
     * USER STATISTICS
     * ============================================================
     *
     * GET /api/users/statistics
     *
     */
    @GetMapping("/statistics")
    public ResponseEntity<UserStatisticsResponse> getStatistics() {



        log.debug(
                "Fetching user statistics"
        );



        UserStatisticsResponse statistics =
                UserStatisticsResponse.builder()

                        .totalUsers(
                                userService.countUsers()
                        )

                        .activeUsers(
                                userService.countActiveUsers()
                        )

                        .disabledUsers(
                                userService.countDisabledUsers()
                        )

                        .admins(
                                userService.countUsersByRole(
                                        Role.ADMIN
                                )
                        )

                        .build();



        return ResponseEntity.ok(statistics);

    }









    /**
     * ============================================================
     * GET USER BY ID
     * ============================================================
     *
     * GET /api/users/{id}
     *
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserManagementResponse> getUserById(

            @PathVariable
            Long id

    ) {


        log.debug(
                "Fetching user id={}",
                id
        );



        return ResponseEntity.ok(

                userService.getUserById(id)

        );

    }









    /**
     * ============================================================
     * CREATE USER
     * ============================================================
     *
     * POST /api/users
     *
     */
    @PostMapping
    public ResponseEntity<UserManagementResponse> createUser(

            @Valid
            @RequestBody
            CreateUserRequest request

    ) {



        log.info(
                "Creating user email={}",
                request.getEmail()
        );



        UserManagementResponse response =
                userService.createUser(request);



        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

    }









    /**
     * ============================================================
     * UPDATE USER
     * ============================================================
     *
     * PUT /api/users/{id}
     *
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserManagementResponse> updateUser(

            @PathVariable
            Long id,


            @Valid
            @RequestBody
            UpdateUserRequest request

    ) {



        log.info(
                "Updating user id={}",
                id
        );



        return ResponseEntity.ok(

                userService.updateUser(
                        id,
                        request
                )

        );

    }









    /**
     * ============================================================
     * ACTIVATE USER
     * ============================================================
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(

            @PathVariable
            Long id

    ) {



        log.info(
                "Activating user id={}",
                id
        );



        userService.activateUser(id);



        return ResponseEntity
                .noContent()
                .build();

    }









    /**
     * ============================================================
     * SUSPEND USER
     * ============================================================
     */
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<Void> suspendUser(

            @PathVariable
            Long id

    ) {



        log.info(
                "Suspending user id={}",
                id
        );



        userService.suspendUser(id);



        return ResponseEntity
                .noContent()
                .build();

    }









    /**
     * ============================================================
     * DELETE USER
     * ============================================================
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(

            @PathVariable
            Long id

    ) {



        log.warn(
                "Deleting user id={}",
                id
        );



        userService.deleteUser(id);



        return ResponseEntity
                .noContent()
                .build();

    }



}