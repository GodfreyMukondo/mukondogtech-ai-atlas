package com.godfrey.ai_immigration_document_analyzer.config;


import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;




/**
 * ============================================================
 * ADMIN BOOTSTRAP SERVICE
 * ============================================================
 *
 * Creates the initial system administrator account.
 *
 * Production features:
 *
 * - Creates admin only when none exists
 * - Uses environment variables
 * - Never stores plain passwords
 * - Uses BCrypt password encoding
 * - Prevents duplicate administrators
 * - Forces first password change
 * - Supports secure deployment
 * - Does not expose credentials in logs
 * - Safe for repeated application restarts
 *
 * ============================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements CommandLineRunner {



    private final UserRepository userRepository;


    private final PasswordEncoder passwordEncoder;


    @Value("${app.admin.email:}")
    private String adminEmail;


    @Value("${app.admin.password:}")
    private String adminPassword;


    /**
     * Executes during application startup.
     *
     * Checks whether an administrator exists.
     *
     * Creates the first administrator only when required.
     */
    @Override
    @Transactional
    public void run(String... args) {


        log.info(
                "Starting administrator bootstrap verification..."
        );


        /*
         * Prevent duplicate administrator creation.
         */
        if (userRepository.existsByRole(Role.ADMIN)) {


            log.info(
                    "Administrator account already exists. Bootstrap skipped."
            );


            return;

        }

        validateConfiguration();

        createAdministrator();

        log.info(
                "Initial administrator bootstrap completed successfully."
        );


    }


    /**
     * Validates required administrator configuration.
     *
     * Values come from:
     *
     * INITIAL_ADMIN_EMAIL
     * INITIAL_ADMIN_PASSWORD
     *
     */
    private void validateConfiguration() {



        if (adminEmail == null || adminEmail.isBlank()) {


            throw new IllegalStateException(
                    "INITIAL_ADMIN_EMAIL is missing. Configure app.admin.email."
            );


        }

        if (adminPassword == null || adminPassword.isBlank()) {


            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD is missing. Configure app.admin.password."
            );


        }





        if (adminPassword.length() < 12) {


            throw new IllegalStateException(
                    "INITIAL_ADMIN_PASSWORD must contain at least 12 characters."
            );


        }


    }









    /**
     * Creates the initial administrator account.
     */
    private void createAdministrator() {



        String normalizedEmail =

                adminEmail
                        .trim()
                        .toLowerCase();







        /*
         * Extra protection.
         *
         * Prevents creating an admin
         * with an existing user email.
         */
        if (userRepository.existsByEmail(normalizedEmail)) {


            throw new IllegalStateException(
                    "Cannot create administrator. Email already exists."
            );


        }







        User admin = new User();






        admin.setEmail(
                normalizedEmail
        );







        admin.setFullName(
                "System Administrator"
        );







        /*
         * Always encrypt passwords.
         *
         * Never save plain text passwords.
         */
        admin.setPassword(

                passwordEncoder.encode(
                        adminPassword.trim()
                )

        );







        admin.setRole(

                Role.ADMIN

        );







        admin.setEnabled(

                true

        );








        /*
         * Security policy:
         *
         * Initial bootstrap password
         * must be changed after
         * first successful login.
         */
        admin.setMustChangePassword(

                true

        );








        userRepository.save(admin);








        /*
         * Security:
         *
         * Never log:
         *
         * - Password
         * - Encoded password
         * - Environment values
         *
         * Only log safe information.
         */
        log.info(
                "Initial administrator created successfully: {}",
                normalizedEmail
        );


    }



}