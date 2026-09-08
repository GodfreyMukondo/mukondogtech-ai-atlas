package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactEvidenceRepository extends JpaRepository<FactEvidence, Long> {

    List<FactEvidence> findByFactId(Long factId);

    long countByFactId(Long factId);
}
