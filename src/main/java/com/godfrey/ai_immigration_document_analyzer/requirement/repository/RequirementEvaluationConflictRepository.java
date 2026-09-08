package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationConflict;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementEvaluationConflictRepository extends JpaRepository<RequirementEvaluationConflict, Long> {

    List<RequirementEvaluationConflict> findByEvaluationId(Long evaluationId);
}
