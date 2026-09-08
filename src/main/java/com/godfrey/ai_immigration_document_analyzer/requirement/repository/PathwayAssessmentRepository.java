package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PathwayAssessmentRepository extends JpaRepository<PathwayAssessment, Long> {

    /** Ownership-scoped lookup - the IDOR-safe pattern for a single assessment. */
    Optional<PathwayAssessment> findByIdAndSubjectUserId(Long id, Long subjectUserId);

    List<PathwayAssessment> findBySubjectUserId(Long subjectUserId);

    List<PathwayAssessment> findBySubjectUserIdAndPathwayId(Long subjectUserId, Long pathwayId);
}
