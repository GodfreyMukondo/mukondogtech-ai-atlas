package com.godfrey.ai_immigration_document_analyzer.service.impl;

import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.UserManagementResponse;


import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.entity.UserStatus;


import com.godfrey.ai_immigration_document_analyzer.exception.DuplicateResourceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;


import com.godfrey.ai_immigration_document_analyzer.repository.ApplicationRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.NotificationRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.PasswordResetTokenRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;
import com.godfrey.ai_immigration_document_analyzer.service.NotificationService;


import com.godfrey.ai_immigration_document_analyzer.service.UserService;


import com.godfrey.ai_immigration_document_analyzer.specification.UserSpecification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


import org.springframework.data.jpa.domain.Specification;


import org.springframework.security.crypto.password.PasswordEncoder;


import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;



import java.util.List;
import java.util.Locale;





/**
 * ============================================================
 * USER SERVICE IMPLEMENTATION
 * ============================================================
 *
 * Enterprise business logic implementation for user management.
 *
 * Supports:
 *
 * - User creation
 * - User updates
 * - User search
 * - User filtering
 * - Pagination
 * - Account activation
 * - Account suspension
 * - User deletion
 * - Statistics
 *
 *
 * Security:
 *
 * - Passwords are encrypted using BCrypt
 * - Entities never exposed directly
 * - Sensitive data never returned
 *
 *
 * Database:
 *
 * Oracle Database 21c
 *
 * ============================================================
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {



    private final UserRepository userRepository;

    private final ApplicationRepository applicationRepository;

    private final DocumentRepository documentRepository;

    private final NotificationRepository notificationRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final NotificationService notificationService;

    private final PasswordEncoder passwordEncoder;




    /**
     * Retrieve users using dynamic filters.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementResponse> getUsers(
            String search,
            Role role,
            UserStatus status,
            Pageable pageable
    ) {


        log.debug(
                "Fetching users search={}, role={}, status={}",
                search,
                role,
                status
        );


        Specification<User> specification =
                UserSpecification.filterUsers(
                        search,
                        role,
                        status
                );


        return userRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(this::mapToResponse);

    }








    /**
     * Retrieve single user.
     */
    @Override
    @Transactional(readOnly = true)
    public UserManagementResponse getUserById(
            Long id
    ) {


        User user =
                findUser(id);


        return mapToResponse(user);

    }








    /**
     * Create user.
     */
    @Override
    public UserManagementResponse createUser(
            CreateUserRequest request
    ) {


        String email =
                normalizeEmail(
                        request.getEmail()
                );



        if(userRepository.existsByEmail(email)) {


            throw new DuplicateResourceException(
                    "Email already exists: " + email
            );

        }



        User user =
                User.builder()

                        .fullName(
                                request.getFullName()
                        )

                        .email(
                                email
                        )

                        .password(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )

                        .phone(
                                request.getPhone()
                        )

                        .country(
                                request.getCountry()
                        )

                        .role(
                                request.getRole()
                        )

                        .enabled(true)

                        .mustChangePassword(false)

                        .build();



        User savedUser =
                userRepository.save(user);



        log.info(
                "Created user id={} email={}",
                savedUser.getId(),
                savedUser.getEmail()
        );



        return mapToResponse(savedUser);

    }








    /**
     * Update existing user.
     */
    @Override
    public UserManagementResponse updateUser(
            Long id,
            UpdateUserRequest request
    ) {


        User user =
                findUser(id);




        if(request.getFullName() != null
                && !request.getFullName().isBlank()) {


            user.setFullName(
                    request.getFullName().trim()
            );

        }




        if(request.getEmail() != null
                && !request.getEmail().isBlank()) {


            String email =
                    normalizeEmail(
                            request.getEmail()
                    );



            if(!email.equals(user.getEmail())
                    &&
                    userRepository.existsByEmail(email)) {


                throw new DuplicateResourceException(
                        "Email already exists: " + email
                );

            }



            user.setEmail(email);

        }





        if(request.getPassword() != null
                && !request.getPassword().isBlank()) {


            user.setPassword(

                    passwordEncoder.encode(
                            request.getPassword()
                    )

            );

            user.completePasswordChange();

        }





        if(request.getRole() != null) {


            user.changeRole(
                    request.getRole()
            );

        }




        if(request.getPhone() != null) {


            user.setPhone(
                    request.getPhone().isBlank()
                            ? null
                            : request.getPhone()
            );

        }




        if(request.getCountry() != null) {


            user.setCountry(
                    request.getCountry().isBlank()
                            ? null
                            : request.getCountry()
            );

        }





        User updatedUser =
                userRepository.save(user);



        log.info(
                "Updated user id={}",
                updatedUser.getId()
        );



        notifyUserOfAdminAction(
                updatedUser.getId(),
                "ACCOUNT_UPDATED",
                "Account Details Updated",
                "An administrator updated your account details.",
                "/dashboard/account"
        );



        return mapToResponse(updatedUser);

    }








    /**
     * Disable account.
     */
    @Override
    public void suspendUser(
            Long id
    ) {


        User user =
                findUser(id);



        user.deactivateAccount();


        userRepository.save(user);



        log.info(
                "Suspended user id={}",
                id
        );



        notifyUserOfAdminAction(
                id,
                "ACCOUNT_SUSPENDED",
                "Account Suspended",
                "Your account has been suspended by an administrator. Contact support if you believe this is a mistake.",
                "/dashboard/account"
        );

    }








    /**
     * Enable account.
     */
    @Override
    public void activateUser(
            Long id
    ) {


        User user =
                findUser(id);



        user.activateAccount();


        userRepository.save(user);



        log.info(
                "Activated user id={}",
                id
        );



        notifyUserOfAdminAction(
                id,
                "ACCOUNT_REACTIVATED",
                "Account Reactivated",
                "Your account has been reactivated. You can now log in again.",
                "/dashboard/account"
        );

    }








    /**
     * Delete user.
     */
    /**
     * Deletes a user together with every record that would otherwise
     * block the delete via a foreign key (documents, applications,
     * notifications, password reset tokens).
     *
     * Order matters: documents must go first because
     * fk_document_application ties them to this user's applications, and
     * everything else must go before the user row itself.
     */
    @Override
    public void deleteUser(
            Long id
    ) {


        User user =
                findUser(id);



        documentRepository.deleteByUserId(id);

        applicationRepository.deleteByUserId(id);

        notificationRepository.deleteByUserId(id);

        passwordResetTokenRepository.deleteByUserId(id);



        userRepository.delete(user);



        log.warn(
                "Deleted user id={} together with their documents, applications, notifications and reset tokens",
                id
        );

    }








    @Override
    @Transactional(readOnly = true)
    public long countUsers() {


        return userRepository.count();

    }








    @Override
    @Transactional(readOnly = true)
    public long countActiveUsers() {


        return userRepository.countByEnabledTrue();

    }








    @Override
    @Transactional(readOnly = true)
    public long countDisabledUsers() {


        return userRepository.countByEnabledFalse();

    }








    @Override
    @Transactional(readOnly = true)
    public long countUsersByRole(
            Role role
    ) {


        return userRepository.countByRole(role);

    }








    @Override
    @Transactional(readOnly = true)
    public boolean emailExists(
            String email
    ) {


        return userRepository.existsByEmail(
                normalizeEmail(email)
        );

    }




    @Override
    @Transactional(readOnly = true)
    public List<UserManagementResponse> getAllUsersForExport() {


        return userRepository
                .findAll(
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                )
                .stream()
                .map(this::mapToResponse)
                .toList();

    }








    /**
     * Notifies a user that an administrator performed an action on their
     * account.
     *
     * Best-effort: the admin action has already been committed, so a
     * notification failure must never fail the action itself.
     */
    private void notifyUserOfAdminAction(
            Long userId,
            String type,
            String title,
            String message,
            String link
    ) {

        try {

            notificationService.notify(
                    userId,
                    type,
                    title,
                    message,
                    link
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Admin action completed but user notification failed | userId={} | type={}",
                    userId,
                    type,
                    ex
            );
        }
    }




    /**
     * Find user or throw exception.
     */
    private User findUser(
            Long id
    ) {


        return userRepository.findById(id)

                .orElseThrow(

                        () ->
                                new ResourceNotFoundException(
                                        "User not found with id: " + id
                                )

                );

    }








    /**
     * Convert entity to response DTO.
     */
    private UserManagementResponse mapToResponse(
            User user
    ) {


        return UserManagementResponse.builder()

                .id(
                        user.getId()
                )

                .name(
                        user.getFullName()
                )

                .email(
                        user.getEmail()
                )

                .phone(
                        user.getPhone()
                )

                .country(
                        user.getCountry()
                )


                .role(
                        user.getRole()
                )


                .status(
                        user.isEnabled()
                                ?
                                UserStatus.ACTIVE
                                :
                                UserStatus.SUSPENDED
                )


                .applications(
                        applicationRepository.countByUserId(
                                user.getId()
                        )
                )


                .joined(
                        user.getCreatedAt()
                )


                .build();

    }








    /**
     * Normalize email.
     */
    private String normalizeEmail(
            String email
    ) {


        return email
                .trim()
                .toLowerCase(Locale.ROOT);

    }



}
