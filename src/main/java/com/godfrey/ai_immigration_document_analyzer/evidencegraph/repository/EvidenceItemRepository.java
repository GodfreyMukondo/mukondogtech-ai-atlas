package com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceItemRepository extends JpaRepository<EvidenceItem, Long> {

    List<EvidenceItem> findByDocumentVersionId(Long documentVersionId);
}
