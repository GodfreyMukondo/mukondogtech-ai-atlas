package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessmentRequirementEvaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PathwayAssessmentRequirementEvaluationRepository
        extends JpaRepository<PathwayAssessmentRequirementEvaluation, Long> {

    List<PathwayAssessmentRequirementEvaluation> findByAssessmentId(Long assessmentId);
}
