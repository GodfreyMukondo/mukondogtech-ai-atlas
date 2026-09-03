package com.godfrey.ai_immigration_document_analyzer.specification;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.entity.UserStatus;


import jakarta.persistence.criteria.Predicate;


import org.springframework.data.jpa.domain.Specification;


import java.util.ArrayList;
import java.util.List;





/**
 * ============================================================
 * USER SPECIFICATION
 * ============================================================
 *
 * Dynamic query builder for User entity.
 *
 * Provides:
 *
 * - User search
 * - Role filtering
 * - Account status filtering
 * - Dynamic database queries
 *
 *
 * Used by:
 *
 * - UserServiceImpl
 * - Admin user management
 * - User search API
 *
 *
 * Database:
 *
 * Oracle Database 21c
 *
 *
 * Architecture:
 *
 * UserServiceImpl
 *        |
 *        v
 * UserSpecification
 *        |
 *        v
 * UserRepository
 *        |
 *        v
 * Oracle Database
 *
 * ============================================================
 */
public final class UserSpecification {



    /**
     * Private constructor.
     *
     * Utility class.
     */
    private UserSpecification() {

    }





    /**
     * Creates dynamic user filtering specification.
     *
     *
     * Supported filters:
     *
     * Search:
     *
     * - fullName
     * - email
     *
     *
     * Role:
     *
     * - ADMIN
     * - USER
     *
     *
     * Status:
     *
     * ACTIVE
     * SUSPENDED
     *
     *
     * @param search search keyword
     * @param role user role
     * @param status account status
     *
     * @return dynamic specification
     */
    public static Specification<User> filterUsers(
            String search,
            Role role,
            UserStatus status
    ) {


        return (root, query, criteriaBuilder) -> {


            List<Predicate> predicates =
                    new ArrayList<>();



            /*
             * ----------------------------------------------------
             * SEARCH FILTER
             * ----------------------------------------------------
             *
             * Searches:
             *
             * FULL_NAME
             * EMAIL
             *
             */
            if(search != null && !search.isBlank()) {


                String keyword =
                        "%" +
                                search.trim().toLowerCase()
                                +
                                "%";



                Predicate nameSearch =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("fullName")
                                ),
                                keyword
                        );



                Predicate emailSearch =
                        criteriaBuilder.like(
                                criteriaBuilder.lower(
                                        root.get("email")
                                ),
                                keyword
                        );



                predicates.add(
                        criteriaBuilder.or(
                                nameSearch,
                                emailSearch
                        )
                );

            }





            /*
             * ----------------------------------------------------
             * ROLE FILTER
             * ----------------------------------------------------
             */
            if(role != null) {


                predicates.add(

                        criteriaBuilder.equal(
                                root.get("role"),
                                role
                        )

                );

            }





            /*
             * ----------------------------------------------------
             * ACCOUNT STATUS FILTER
             * ----------------------------------------------------
             *
             * User entity stores status as:
             *
             * enabled = true / false
             *
             */
            if(status != null) {


                switch(status) {


                    case ACTIVE -> predicates.add(

                            criteriaBuilder.equal(
                                    root.get("enabled"),
                                    true
                            )

                    );



                    case SUSPENDED -> predicates.add(

                            criteriaBuilder.equal(
                                    root.get("enabled"),
                                    false
                            )

                    );


                    default -> {
                        // No additional filter
                    }

                }

            }





            /*
             * ----------------------------------------------------
             * FINAL QUERY
             * ----------------------------------------------------
             */
            return criteriaBuilder.and(

                    predicates.toArray(
                            new Predicate[0]
                    )

            );

        };

    }



}