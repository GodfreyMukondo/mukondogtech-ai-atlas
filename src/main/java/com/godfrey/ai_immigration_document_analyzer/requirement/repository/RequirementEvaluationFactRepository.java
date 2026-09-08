package com.godfrey.ai_immigration_document_analyzer.requirement.repository;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationFact;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementEvaluationFactRepository extends JpaRepository<RequirementEvaluationFact, Long> {

    List<RequirementEvaluationFact> findByEvaluationId(Long evaluationId);
}
