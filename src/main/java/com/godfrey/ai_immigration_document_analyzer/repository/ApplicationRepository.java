package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.Application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * ============================================================================
 * APPLICATION REPOSITORY
 * ============================================================================
 */
@Repository
public interface ApplicationRepository
        extends JpaRepository<Application, Long> {


    // =========================================================================
    // USER APPLICATIONS
    // =========================================================================

    List<Application> findByUserIdOrderBySubmittedAtDesc(
            Long userId
    );


    /**
     * Secure ownership-aware application lookup.
     */
    Optional<Application> findByIdAndUserId(
            Long id,
            Long userId
    );


    long countByUserId(
            Long userId
    );


    /**
     * Removes every application submitted by the user. Callers must delete
     * that user's documents first (fk_document_application references
     * this table).
     */
    void deleteByUserId(
            Long userId
    );


    // =========================================================================
    // ADMIN LISTING
    // =========================================================================

    List<Application> findAllByOrderBySubmittedAtDesc();


    // =========================================================================
    // STATISTICS
    // =========================================================================

    long countByStatus(
            String status
    );


    long countByRiskLevel(
            String riskLevel
    );


    /**
     * Counts applications in the given status that have been sitting since
     * before the given cutoff - used by the AI audit to flag stale cases
     * needing review.
     */
    long countByStatusAndSubmittedAtBefore(
            String status,
            LocalDateTime cutoff
    );


    /*
    |--------------------------------------------------------------------------
    | GROWTH ANALYTICS
    |--------------------------------------------------------------------------
    */

    /**
     * Count applications submitted within a date range.
     *
     * Used to compute real period-over-period growth for the admin
     * dashboard's "active cases" KPI, rather than a fabricated constant.
     */
    long countBySubmittedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );


    /*
    |--------------------------------------------------------------------------
    | REGIONAL BREAKDOWN
    |--------------------------------------------------------------------------
    */

    /**
     * Number of distinct destination countries applications have been
     * submitted for - the platform's real "countries covered" figure.
     */
    @Query(
            "SELECT COUNT(DISTINCT a.country) FROM Application a"
    )
    long countDistinctCountry();


    /**
     * Application volume grouped by destination country, most popular
     * first - backs the admin dashboard's regional breakdown.
     */
    @Query(
            "SELECT a.country AS country, COUNT(a) AS total "
                    + "FROM Application a "
                    + "GROUP BY a.country "
                    + "ORDER BY COUNT(a) DESC"
    )
    List<CountryApplicationCount> countGroupedByCountry();


    /**
     * Projection for {@link #countGroupedByCountry()}.
     */
    interface CountryApplicationCount {

        String getCountry();

        long getTotal();
    }
}
