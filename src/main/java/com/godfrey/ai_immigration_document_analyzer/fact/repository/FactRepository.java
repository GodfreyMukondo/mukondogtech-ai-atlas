package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * FACT REPOSITORY
 * ============================================================================
 *
 * All access is subject-scoped by convention, matching the ownership-safe
 * {@code findByIdAndUserId} pattern already used elsewhere in this codebase
 * (e.g. {@code DocumentRepository}). No caller outside the {@code fact}
 * service package should query this repository directly - authorization is
 * enforced in {@code FactAuthorizationService} before any of these methods
 * are invoked.
 * ============================================================================
 */
@Repository
public interface FactRepository extends JpaRepository<Fact, Long> {

    /** Ownership-scoped lookup - the IDOR-safe pattern for a single Fact. */
    Optional<Fact> findByIdAndSubjectUserId(Long id, Long subjectUserId);

    List<Fact> findBySubjectUserId(Long subjectUserId);

    List<Fact> findBySubjectUserIdAndStatus(Long subjectUserId, FactStatus status);

    List<Fact> findBySubjectUserIdAndFactKeyAndStatus(
            Long subjectUserId,
            String factKey,
            FactStatus status
    );

    List<Fact> findBySubjectUserIdAndFactKeyAndStatusIn(
            Long subjectUserId,
            String factKey,
            List<FactStatus> statuses
    );

    /** The currently-open-ended fact of a historical-multivalued key, if any. */
    Optional<Fact> findBySubjectUserIdAndFactKeyAndStatusAndEffectiveToIsNull(
            Long subjectUserId,
            String factKey,
            FactStatus status
    );
}
