package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.CaseAssignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Backs the CASE_WORKER / SYSTEM_ADMINISTRATOR separation: holding the
 * {@code ADMIN} role grants no Fact access by itself - only an active row
 * here (held by a CASE_WORKER-role user) does. See
 * {@code FactAuthorizationService}.
 */
@Repository
public interface CaseAssignmentRepository extends JpaRepository<CaseAssignment, Long> {

    boolean existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(
            Long caseWorkerUserId,
            Long subjectUserId
    );

    List<CaseAssignment> findByCaseWorkerUserIdAndActiveTrue(Long caseWorkerUserId);

    List<CaseAssignment> findBySubjectUserIdAndActiveTrue(Long subjectUserId);
}
