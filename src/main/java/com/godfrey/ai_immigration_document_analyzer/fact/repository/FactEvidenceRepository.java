package com.godfrey.ai_immigration_document_analyzer.fact.repository;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactEvidenceRepository extends JpaRepository<FactEvidence, Long> {

    List<FactEvidence> findByFactId(Long factId);

    long countByFactId(Long factId);

    /**
     * Reverse lookup used by the Evidence Intelligence Graph
     * (EvidenceGraphService.getEvidenceItemDetail) to find every Fact one
     * EvidenceItem is linked to, since EvidenceItem carries no
     * subjectUserId of its own and must be authorized via the Facts it
     * actually supports.
     */
    List<FactEvidence> findByEvidenceItemId(Long evidenceItemId);
}
