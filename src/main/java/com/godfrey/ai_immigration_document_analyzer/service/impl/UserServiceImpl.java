package com.godfrey.ai_immigration_document_analyzer.service.impl;

import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.UserManagementResponse;


import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.entity.UserStatus;


import com.godfrey.ai_immigration_document_analyzer.exception.DuplicateResourceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;


import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;


import com.godfrey.ai_immigration_document_analyzer.service.UserService;


import com.godfrey.ai_immigration_document_analyzer.specification.UserSpecification;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import org.springframework.data.jpa.domain.Specification;


import org.springframework.security.crypto.password.PasswordEncoder;


import org.springframework.stereotype.Service;


import org.springframework.transaction.annotation.Transactional;



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





        User updatedUser =
                userRepository.save(user);



        log.info(
                "Updated user id={}",
                updatedUser.getId()
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

    }








    /**
     * Delete user.
     */
    @Override
    public void deleteUser(
            Long id
    ) {


        User user =
                findUser(id);



        userRepository.delete(user);



        log.warn(
                "Deleted user id={}",
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

                /*
                 * User entity does not currently
                 * contain these fields.
                 */
                .phone(null)

                .country(null)


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


                /*
                 * Application module not connected yet.
                 */
                .applications(0L)


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
