package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    List<Requirement> findByRequirementKey(String requirementKey);

    /**
     * The Requirement row expressing {@code requirementKey} whose
     * RegulatoryVersion was in force at {@code assessmentDate} - the
     * mechanism that lets a past evaluation use the rule that actually
     * existed then, never today's rule (section 13).
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT r FROM Requirement r JOIN RegulatoryVersion v ON r.regulatoryVersionId = v.id "
                    + "WHERE r.requirementKey = :requirementKey "
                    + "AND v.effectiveFrom <= :assessmentDate AND (v.effectiveTo IS NULL OR v.effectiveTo > :assessmentDate)"
    )
    Optional<Requirement> findEffectiveAt(String requirementKey, LocalDateTime assessmentDate);

    List<Requirement> findByStatus(RequirementStatus status);
}
