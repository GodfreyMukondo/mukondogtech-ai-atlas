package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequirementEvaluationRepository extends JpaRepository<RequirementEvaluation, Long> {

    /** Ownership-scoped lookup - the IDOR-safe pattern for a single evaluation. */
    Optional<RequirementEvaluation> findByIdAndSubjectUserId(Long id, Long subjectUserId);

    List<RequirementEvaluation> findBySubjectUserId(Long subjectUserId);
}
